package com.taxipot.domain.wallet.service;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.WalletAdjustRequest;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.PointType;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 운영자 지갑 잔액 조정 (PATCH /api/admin/users/{userId}/wallet) 단위 테스트.
 * <p>WalletLedger·DebtAutoPayer·WalletResponseFactory 는 진짜 객체를 쓰고, DB(Repository)·락·시간만 가짜(Mock)로 바꾼다.
 * 그래서 "지갑 금액 + 원장 기록 + 미수금"이 명세대로 함께 바뀌는지 DB 없이 확인할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminWalletServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 21, 0);

    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock MemberWalletLocker memberWalletLocker;
    @Mock TimeProvider timeProvider;
    @Mock AppProperties appProperties;

    AdminWalletService service;
    Wallet wallet;
    List<Settlement> debts;

    @BeforeEach
    void setUp() {
        WalletLedger ledger = new WalletLedger(walletRepository, pointHistoryRepository, appProperties);
        DebtAutoPayer autoPayer = new DebtAutoPayer(settlementRepository, ledger);
        WalletResponseFactory responseFactory = new WalletResponseFactory(settlementRepository, walletRepository);
        service = new AdminWalletService(memberWalletLocker, ledger, autoPayer, responseFactory, timeProvider);

        wallet = Wallet.open(USER_ID);
        debts = new ArrayList<>();

        when(memberWalletLocker.lock(USER_ID)).thenReturn(new LockedMember(null, wallet));
        when(appProperties.wallet()).thenReturn(new AppProperties.Wallet(500_000, 10_000, 50_000));
        when(timeProvider.now()).thenReturn(NOW);
        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenAnswer(inv -> debts.stream().filter(Settlement::hasDebt).toList());
    }

    /** 미수금 행 만들기: 예치 7,000 → 정산에서 uncovered 만큼 못 걷음 */
    private Settlement debt(Long potId, int uncovered, LocalDateTime dueAt) {
        Settlement s = Settlement.hold(potId, USER_ID, 7_000);
        s.settle(10_000, 1_000, 7_000 - 11_000, uncovered, NOW.minusDays(5), dueAt);
        return s;
    }

    private WalletResponse adjust(int amount) {
        return service.adjustWallet(ADMIN_ID, USER_ID, new WalletAdjustRequest(amount));
    }

    private List<PointHistory> savedHistories() {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    // ===================================================================== 입금(+)

    @Test
    @DisplayName("+20,000 충전: 잔액 0 → 20,000, 원장 ADMIN_ADJUST +20,000 (payment_id 없음)")
    void credit() {
        WalletResponse res = adjust(20_000);

        assertThat(wallet.getBalance()).isEqualTo(20_000);
        assertThat(res.balance()).isEqualTo(20_000);
        assertThat(res.debtAmount()).isZero();

        List<PointHistory> histories = savedHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PointType.ADMIN_ADJUST);
        assertThat(histories.get(0).getAmount()).isEqualTo(20_000);
        assertThat(histories.get(0).getBalanceAfter()).isEqualTo(20_000);
        assertThat(histories.get(0).getPaymentId()).isNull();
    }

    @Test
    @DisplayName("상한 초과: 잔액 490,000에 +20,000 → 409 WALLET_LIMIT_EXCEEDED, 지갑·원장 그대로")
    void credit_overLimit() {
        wallet.deposit(490_000);

        assertThatThrownBy(() -> adjust(20_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WALLET_LIMIT_EXCEEDED);

        assertThat(wallet.getBalance()).isEqualTo(490_000);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("상한은 예치금까지 합쳐서 본다: 잔액 400,000 + 예치 100,000이면 +1원도 초과")
    void credit_overLimit_includesHeld() {
        wallet.deposit(500_000);
        wallet.hold(100_000);   // 잔액 400,000 / 예치 100,000

        assertThatThrownBy(() -> adjust(1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WALLET_LIMIT_EXCEEDED);
    }

    // ===================================================================== 차감(−)

    @Test
    @DisplayName("−5,000 차감: 잔액 10,000 → 5,000, 원장 ADMIN_ADJUST −5,000")
    void debit() {
        wallet.deposit(10_000);

        WalletResponse res = adjust(-5_000);

        assertThat(res.balance()).isEqualTo(5_000);
        List<PointHistory> histories = savedHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PointType.ADMIN_ADJUST);
        assertThat(histories.get(0).getAmount()).isEqualTo(-5_000);
        assertThat(histories.get(0).getBalanceAfter()).isEqualTo(5_000);
    }

    @Test
    @DisplayName("잔액보다 많이 차감: 잔액 3,000에 −5,000 → 409 INSUFFICIENT_BALANCE, 잔액 그대로")
    void debit_insufficient() {
        wallet.deposit(3_000);

        assertThatThrownBy(() -> adjust(-5_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(wallet.getBalance()).isEqualTo(3_000);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("차감(−)할 때는 미수금 자동 납부를 하지 않는다")
    void debit_doesNotAutoPay() {
        wallet.deposit(10_000);
        debts.add(debt(100L, 3_000, NOW.plusDays(1)));

        WalletResponse res = adjust(-1_000);

        assertThat(debts.get(0).getDebtAmount()).isEqualTo(3_000);
        assertThat(res.debtAmount()).isEqualTo(3_000);
    }

    // ===================================================================== 미수금 자동 납부

    @Test
    @DisplayName("+6,000 충전 → 미수금 자동 납부: 기한 빠른 3,000 완납(PAID), 다음 5,000 중 3,000 부분 납부")
    void credit_autoPaysDebts_inDueOrder() {
        Settlement overdue = debt(100L, 3_000, NOW.minusDays(1));
        overdue.markDebtOverdue();                                   // 기한 지난 미수금
        Settlement unpaid = debt(200L, 5_000, NOW.plusDays(2));      // 아직 기한 남은 미수금
        debts.add(overdue);
        debts.add(unpaid);

        WalletResponse res = adjust(6_000);

        // 미수금: 첫 번째 완납, 두 번째 2,000 남음
        assertThat(overdue.getDebtStatus()).isEqualTo(DebtStatus.PAID);
        assertThat(overdue.getDebtAmount()).isZero();
        assertThat(overdue.getDebtPaidAt()).isEqualTo(NOW);
        assertThat(unpaid.getDebtStatus()).isEqualTo(DebtStatus.UNPAID);
        assertThat(unpaid.getDebtAmount()).isEqualTo(2_000);

        // 지갑: 충전한 6,000을 전부 미수금에 씀
        assertThat(res.balance()).isZero();
        assertThat(res.debtAmount()).isEqualTo(2_000);

        // 원장: ADMIN_ADJUST +6,000 → DEBT_PAY −3,000(방 100) → DEBT_PAY −3,000(방 200)
        List<PointHistory> histories = savedHistories();
        assertThat(histories).extracting(PointHistory::getType)
                .containsExactly(PointType.ADMIN_ADJUST, PointType.DEBT_PAY, PointType.DEBT_PAY);
        assertThat(histories).extracting(PointHistory::getAmount)
                .containsExactly(6_000, -3_000, -3_000);
        assertThat(histories).extracting(PointHistory::getBalanceAfter)
                .containsExactly(6_000, 3_000, 0);
        assertThat(histories).extracting(PointHistory::getPotId)
                .containsExactly(null, 100L, 200L);
    }

    @Test
    @DisplayName("충전액이 미수금보다 많으면 미수금을 다 갚고 남은 돈은 잔액으로 남는다")
    void credit_autoPay_leftover() {
        debts.add(debt(100L, 3_000, NOW.plusDays(1)));

        WalletResponse res = adjust(10_000);

        assertThat(debts.get(0).getDebtStatus()).isEqualTo(DebtStatus.PAID);
        assertThat(res.balance()).isEqualTo(7_000);
        assertThat(res.debtAmount()).isZero();
    }

    @Test
    @DisplayName("0원 조정: 아무것도 바뀌지 않고 원장도 안 남는다")
    void zero() {
        wallet.deposit(10_000);

        WalletResponse res = adjust(0);

        assertThat(res.balance()).isEqualTo(10_000);
        verify(pointHistoryRepository, never()).save(any());
    }
}
