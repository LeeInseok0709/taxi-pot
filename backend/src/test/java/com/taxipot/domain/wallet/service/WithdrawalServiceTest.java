package com.taxipot.domain.wallet.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.dto.WithdrawalPageResponse;
import com.taxipot.domain.wallet.dto.WithdrawalRequest;
import com.taxipot.domain.wallet.dto.WithdrawalResponse;
import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.entity.Withdrawal;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.domain.wallet.repository.WithdrawalRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PointType;
import com.taxipot.global.code.WithdrawalStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 출금·출금 내역 (POST·GET /api/wallet/withdrawals) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawalServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long WITHDRAWAL_ID = 31L;
    private static final int FAIL_AMOUNT = 7_777;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 31, 2, 0);

    @Mock MemberWalletLocker memberWalletLocker;
    @Mock WithdrawalRepository withdrawalRepository;
    @Mock NotificationSender notificationSender;
    @Mock AppProperties appProperties;
    @Mock TimeProvider timeProvider;
    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    WithdrawalService service;
    Wallet wallet;
    User user;

    @BeforeEach
    void setUp() {
        WalletLedger ledger = new WalletLedger(walletRepository, pointHistoryRepository, appProperties);
        service = new WithdrawalService(memberWalletLocker, ledger, withdrawalRepository, notificationSender,
                appProperties, timeProvider);

        wallet = Wallet.open(USER_ID);
        user = mock(User.class);
        when(user.hasAccount()).thenReturn(true);
        when(user.getBankCode()).thenReturn("004");            // KB국민은행
        when(user.getAccountNo()).thenReturn("1234567890");
        when(user.getAccountHolder()).thenReturn("홍길동");

        when(memberWalletLocker.lock(USER_ID)).thenReturn(new LockedMember(user, wallet));
        when(appProperties.withdrawal()).thenReturn(new AppProperties.Withdrawal(1_000, FAIL_AMOUNT));
        when(appProperties.wallet()).thenReturn(new AppProperties.Wallet(500_000, 10_000, 50_000));
        when(timeProvider.now()).thenReturn(NOW);
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> {
            Withdrawal w = inv.getArgument(0);
            ReflectionTestUtils.setField(w, "id", WITHDRAWAL_ID);   // DB가 번호를 붙여 준 것처럼
            return w;
        });
    }

    private WithdrawalResponse withdraw(int amount) {
        return service.requestWithdrawal(USER_ID, new WithdrawalRequest(amount));
    }

    private List<PointHistory> savedHistories() {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    // ===================================================================== 출금 성공

    @Test
    @DisplayName("출금 성공: 잔액 30,000에서 10,000 출금 → COMPLETED, 잔액 20,000, 원장 WITHDRAW −10,000, 입금 알림")
    void withdraw_success() {
        wallet.deposit(30_000);

        WithdrawalResponse res = withdraw(10_000);

        assertThat(res.id()).isEqualTo(WITHDRAWAL_ID);
        assertThat(res.status()).isEqualTo(WithdrawalStatus.COMPLETED);
        assertThat(res.transferTid()).startsWith("DUMMY-");
        assertThat(res.transferProvider()).isEqualTo("DUMMY");
        assertThat(res.balanceAfter()).isEqualTo(20_000);
        assertThat(res.bankName()).isEqualTo("KB국민은행");
        assertThat(res.maskedAccountNumber()).isEqualTo("123-****-7890");   // 계좌번호는 마스킹
        assertThat(res.accountHolder()).isEqualTo("홍길동");
        assertThat(res.completedAt()).isNotNull();

        List<PointHistory> histories = savedHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PointType.WITHDRAW);
        assertThat(histories.get(0).getAmount()).isEqualTo(-10_000);
        assertThat(histories.get(0).getWithdrawalId()).isEqualTo(WITHDRAWAL_ID);

        verify(notificationSender).send(eq(USER_ID), isNull(), eq(NotificationType.PAYMENT), eq("출금 완료"),
                eq("10,000원이 KB국민은행 123-****-7890 계좌로 입금되었습니다."));
    }

    // ===================================================================== 송금 실패 (시연용)

    @Test
    @DisplayName("더미 송금 실패: 실패 금액으로 요청 → FAILED, 잔액 원래대로, 원장 WITHDRAW → WITHDRAW_REFUND, 실패 알림")
    void withdraw_dummyFailure() {
        wallet.deposit(30_000);

        WithdrawalResponse res = withdraw(FAIL_AMOUNT);

        assertThat(res.status()).isEqualTo(WithdrawalStatus.FAILED);
        assertThat(res.failReason()).isNotBlank();
        assertThat(res.balanceAfter()).isEqualTo(30_000);       // 돌려받아서 원래 잔액
        assertThat(wallet.getBalance()).isEqualTo(30_000);

        List<PointHistory> histories = savedHistories();
        assertThat(histories).extracting(PointHistory::getType)
                .containsExactly(PointType.WITHDRAW, PointType.WITHDRAW_REFUND);
        assertThat(histories).extracting(PointHistory::getAmount)
                .containsExactly(-FAIL_AMOUNT, FAIL_AMOUNT);
        assertThat(histories).extracting(PointHistory::getWithdrawalId)
                .containsExactly(WITHDRAWAL_ID, WITHDRAWAL_ID);  // 같은 출금 번호로 연결

        verify(notificationSender).send(eq(USER_ID), isNull(), eq(NotificationType.PAYMENT), eq("출금 실패"), anyString());
    }

    // ===================================================================== 검증 실패

    @Test
    @DisplayName("계좌 미등록: 409 ACCOUNT_NOT_REGISTERED, 출금 기록·잔액 변화 없음")
    void withdraw_noAccount() {
        wallet.deposit(30_000);
        when(user.hasAccount()).thenReturn(false);

        assertThatThrownBy(() -> withdraw(10_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_REGISTERED);

        verify(withdrawalRepository, never()).save(any());
        assertThat(wallet.getBalance()).isEqualTo(30_000);
    }

    @Test
    @DisplayName("1,000원 미만: 400 INVALID_INPUT")
    void withdraw_belowMinimum() {
        wallet.deposit(30_000);

        assertThatThrownBy(() -> withdraw(999))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("잔액보다 많이: 409 INSUFFICIENT_BALANCE")
    void withdraw_insufficient() {
        wallet.deposit(5_000);

        assertThatThrownBy(() -> withdraw(6_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(withdrawalRepository, never()).save(any());
    }

    @Test
    @DisplayName("예치금은 출금 대상 아님: 잔액 5,000 + 예치 20,000이어도 6,000 출금은 409")
    void withdraw_heldBalanceNotWithdrawable() {
        wallet.deposit(25_000);
        wallet.hold(20_000);   // 잔액 5,000 / 예치 20,000

        assertThatThrownBy(() -> withdraw(6_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // ===================================================================== 출금 내역

    @Test
    @DisplayName("출금 내역: id 내림차순, 계좌번호 마스킹, balanceAfter는 null")
    void getMyWithdrawals() {
        Withdrawal w = Withdrawal.request(USER_ID, 10_000, "004", "1234567890", "홍길동");
        ReflectionTestUtils.setField(w, "id", WITHDRAWAL_ID);
        w.complete("DUMMY-abc", NOW);
        when(withdrawalRepository.findAllByMemberId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(w), inv.getArgument(1), 1));

        WithdrawalPageResponse res = service.getMyWithdrawals(USER_ID, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(withdrawalRepository).findAllByMemberId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));

        assertThat(res.content()).hasSize(1);
        WithdrawalResponse first = res.content().get(0);
        assertThat(first.maskedAccountNumber()).isEqualTo("123-****-7890");
        assertThat(first.bankName()).isEqualTo("KB국민은행");
        assertThat(first.balanceAfter()).isNull();
        assertThat(first.status()).isEqualTo(WithdrawalStatus.COMPLETED);
        assertThat(res.page().totalElements()).isEqualTo(1L);
    }
}
