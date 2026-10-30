package com.taxipot.domain.wallet.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.DebtPayResponse;
import com.taxipot.domain.wallet.dto.DebtResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 내 미수금 목록·미수금 납부 (GET /api/wallet/debts, POST /api/wallet/debts/{id}/pay) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DebtServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long SETTLEMENT_ID = 55L;
    private static final LocalDateTime SETTLED_AT = LocalDateTime.of(2026, 10, 28, 22, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 12, 0);

    @Mock SettlementRepository settlementRepository;
    @Mock TaxipotRepository taxipotRepository;
    @Mock MemberWalletLocker memberWalletLocker;
    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @Mock AppProperties appProperties;
    @Mock TimeProvider timeProvider;

    DebtService service;
    Wallet wallet;

    @BeforeEach
    void setUp() {
        WalletLedger ledger = new WalletLedger(walletRepository, pointHistoryRepository, appProperties);
        service = new DebtService(settlementRepository, taxipotRepository, memberWalletLocker, ledger, timeProvider);

        wallet = Wallet.open(USER_ID);
        when(memberWalletLocker.lock(USER_ID)).thenReturn(new LockedMember(null, wallet));
        when(timeProvider.now()).thenReturn(NOW);
    }

    private Settlement debt(Long potId, Long memberId, int uncovered, LocalDateTime dueAt) {
        Settlement s = Settlement.hold(potId, memberId, 7_000);
        s.settle(10_000, 1_000, -4_000, uncovered, SETTLED_AT, dueAt);
        return s;
    }

    private Taxipot pot(Long id, String title) {
        Taxipot pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(id);
        when(pot.getTitle()).thenReturn(title);
        return pot;
    }

    // ===================================================================== GET /api/wallet/debts

    @Test
    @DisplayName("미수금 목록: 납부 기한 순서 그대로, 방 제목·남은 금액·기한(+09:00)을 보여준다")
    void getMyDebts() {
        Settlement overdue = debt(100L, USER_ID, 3_000, SETTLED_AT.plusDays(3));
        overdue.markDebtOverdue();
        overdue.payDebt(1_000, SETTLED_AT.plusDays(4));              // 일부만 납부 → 남은 2,000
        Settlement unpaid = debt(200L, USER_ID, 5_000, SETTLED_AT.plusDays(5));

        // 가짜 방은 when(...) 밖에서 먼저 만든다 (stubbing 도중에 다른 stubbing을 하면 Mockito 에러)
        Taxipot pot100 = pot(100L, "두정역 → 본관");
        Taxipot pot200 = pot(200L, "터미널 → 기숙사");

        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(overdue, unpaid));
        when(taxipotRepository.findAllById(anyIterable())).thenReturn(List.of(pot100, pot200));

        List<DebtResponse> res = service.getMyDebts(USER_ID);

        assertThat(res).hasSize(2);
        DebtResponse first = res.get(0);
        assertThat(first.potId()).isEqualTo(100L);
        assertThat(first.potTitle()).isEqualTo("두정역 → 본관");
        assertThat(first.debtStatus()).isEqualTo(DebtStatus.OVERDUE);
        assertThat(first.uncoveredAmount()).isEqualTo(3_000);   // 정산 당시 못 걷은 금액
        assertThat(first.debtAmount()).isEqualTo(2_000);        // 지금 남은 금액
        assertThat(first.debtDueAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(first.settledAt().toLocalDateTime()).isEqualTo(SETTLED_AT);

        assertThat(res.get(1).potTitle()).isEqualTo("터미널 → 기숙사");
        assertThat(res.get(1).debtStatus()).isEqualTo(DebtStatus.UNPAID);
    }

    @Test
    @DisplayName("미수금이 없으면 빈 목록 (방 조회도 안 함)")
    void getMyDebts_empty() {
        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of());

        assertThat(service.getMyDebts(USER_ID)).isEmpty();
        verify(taxipotRepository, never()).findAllById(anyIterable());
    }

    // ===================================================================== POST /api/wallet/debts/{id}/pay

    private Settlement myDebt(int amount) {
        Settlement s = debt(100L, USER_ID, amount, SETTLED_AT.plusDays(3));
        ReflectionTestUtils.setField(s, "id", SETTLEMENT_ID);
        when(settlementRepository.findByIdForUpdate(SETTLEMENT_ID)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    @DisplayName("미수금 납부: 잔액 10,000으로 미수금 3,000 완납 → PAID, 잔액 7,000, 원장 DEBT_PAY −3,000")
    void payDebt_full() {
        wallet.deposit(10_000);
        Settlement s = myDebt(3_000);

        DebtPayResponse res = service.payDebt(USER_ID, SETTLEMENT_ID);

        assertThat(res.settlementId()).isEqualTo(SETTLEMENT_ID);
        assertThat(res.paidAmount()).isEqualTo(3_000);
        assertThat(res.debtStatus()).isEqualTo(DebtStatus.PAID);
        assertThat(res.debtAmount()).isZero();
        assertThat(res.balanceAfter()).isEqualTo(7_000);
        assertThat(s.getDebtPaidAt()).isEqualTo(NOW);

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointType.DEBT_PAY);
        assertThat(captor.getValue().getAmount()).isEqualTo(-3_000);
        assertThat(captor.getValue().getPotId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("미수금 납부: 잔액 1,200뿐이면 1,200만 부분 납부, 남은 1,800은 그대로 UNPAID")
    void payDebt_partial() {
        wallet.deposit(1_200);
        myDebt(3_000);

        DebtPayResponse res = service.payDebt(USER_ID, SETTLEMENT_ID);

        assertThat(res.paidAmount()).isEqualTo(1_200);
        assertThat(res.debtStatus()).isEqualTo(DebtStatus.UNPAID);
        assertThat(res.debtAmount()).isEqualTo(1_800);
        assertThat(res.balanceAfter()).isZero();
    }

    @Test
    @DisplayName("미수금 납부: 잔액 0원이면 409 INSUFFICIENT_BALANCE (requiredAmount = 남은 미수금)")
    void payDebt_zeroBalance() {
        myDebt(3_000);

        assertThatThrownBy(() -> service.payDebt(USER_ID, SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(be.getDetail()).containsEntry("requiredAmount", 3_000);
                });
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("미수금 납부: 이미 다 갚은(PAID) 행이면 409 NO_DEBT")
    void payDebt_noDebt() {
        wallet.deposit(10_000);
        Settlement s = myDebt(3_000);
        s.payDebt(3_000, NOW);   // 이미 완납

        assertThatThrownBy(() -> service.payDebt(USER_ID, SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NO_DEBT);
    }

    @Test
    @DisplayName("미수금 납부: 남의 정산 행이면 404 NOT_FOUND")
    void payDebt_othersSettlement() {
        wallet.deposit(10_000);
        Settlement others = debt(100L, 999L, 3_000, SETTLED_AT.plusDays(3));
        when(settlementRepository.findByIdForUpdate(SETTLEMENT_ID)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.payDebt(USER_ID, SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
