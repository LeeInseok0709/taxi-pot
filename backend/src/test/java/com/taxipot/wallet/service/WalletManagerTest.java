package com.taxipot.wallet.service;

import com.taxipot.global.code.PointType;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.wallet.entity.PointHistory;
import com.taxipot.wallet.entity.Settlement;
import com.taxipot.wallet.entity.Wallet;
import com.taxipot.wallet.repository.PointHistoryRepository;
import com.taxipot.wallet.repository.SettlementRepository;
import com.taxipot.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletManager.hold / refundHold 단위 테스트.
 * DB 없이 Repository를 가짜(Mock)로 바꿔서, 지갑 잔액·원장·정산 행이 명세대로 바뀌는지만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class WalletManagerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POT_ID = 10L;

    @Mock WalletRepository walletRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    WalletManager walletManager;

    @BeforeEach
    void setUp() {
        // AppProperties는 hold·refundHold에서 안 쓰므로 null
        walletManager = new WalletManager(walletRepository, null, settlementRepository, pointHistoryRepository);
    }

    /** 잔액·예치금을 지정한 지갑 만들기 (Setter가 없어서 테스트에서만 필드를 직접 넣는다) */
    private Wallet wallet(int balance, int held) {
        Wallet wallet = Wallet.builder().memberId(MEMBER_ID).build();
        ReflectionTestUtils.setField(wallet, "balance", balance);
        ReflectionTestUtils.setField(wallet, "heldBalance", held);
        return wallet;
    }

    private Settlement settlement(int heldAmount, SettlementStatus status) {
        Settlement settlement = Settlement.builder().potId(POT_ID).memberId(MEMBER_ID).heldAmount(heldAmount).build();
        ReflectionTestUtils.setField(settlement, "status", status);
        return settlement;
    }

    private PointHistory savedHistory() {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================================================================== hold

    @Test
    @DisplayName("hold: 처음 참여 — 잔액 10,000 → 7,000 예치, 원장 FARE_HOLD −7,000, 정산 행 새로 생성")
    void hold_firstJoin() {
        Wallet wallet = wallet(10_000, 0);
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.empty());
        when(walletRepository.findByMemberIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(wallet));

        walletManager.hold(MEMBER_ID, POT_ID, 7_000);

        assertThat(wallet.getBalance()).isEqualTo(3_000);
        assertThat(wallet.getHeldBalance()).isEqualTo(7_000);

        PointHistory history = savedHistory();
        assertThat(history.getType()).isEqualTo(PointType.FARE_HOLD);
        assertThat(history.getAmount()).isEqualTo(-7_000);
        assertThat(history.getBalanceAfter()).isEqualTo(3_000);
        assertThat(history.getPotId()).isEqualTo(POT_ID);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SettlementStatus.HELD);
        assertThat(captor.getValue().getHeldAmount()).isEqualTo(7_000);
    }

    @Test
    @DisplayName("hold: 잔액 부족 — 409 INSUFFICIENT_BALANCE, detail에 부족 금액, 지갑·원장 변화 없음")
    void hold_insufficientBalance() {
        Wallet wallet = wallet(2_500, 0);
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.empty());
        when(walletRepository.findByMemberIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletManager.hold(MEMBER_ID, POT_ID, 7_000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(be.getMessage()).isEqualTo("지갑 잔액이 4,500원 부족합니다.");
                    assertThat(be.getDetail())
                            .containsEntry("requiredAmount", 7_000)
                            .containsEntry("balance", 2_500)
                            .containsEntry("shortageAmount", 4_500);
                });

        assertThat(wallet.getBalance()).isEqualTo(2_500);
        assertThat(wallet.getHeldBalance()).isZero();
        verify(pointHistoryRepository, never()).save(any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("hold: 취소했던 방 재참여 — 새 행 없이 기존 정산 행을 다시 HELD로")
    void hold_rejoinAfterCancel() {
        Wallet wallet = wallet(10_000, 0);
        Settlement canceled = settlement(5_000, SettlementStatus.REFUNDED);
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.of(canceled));
        when(walletRepository.findByMemberIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(wallet));

        walletManager.hold(MEMBER_ID, POT_ID, 7_000);

        assertThat(canceled.getStatus()).isEqualTo(SettlementStatus.HELD);
        assertThat(canceled.getHeldAmount()).isEqualTo(7_000);   // 새 예치액으로 덮어씀
        verify(settlementRepository, never()).save(any());        // UNIQUE(pot, member)라 새 행 금지
        assertThat(wallet.getBalance()).isEqualTo(3_000);
    }

    @Test
    @DisplayName("hold: 이미 예치 중인 방에 또 예치 — 409 ALREADY_JOINED, 지갑 안 건드림")
    void hold_duplicate() {
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID))
                .thenReturn(Optional.of(settlement(7_000, SettlementStatus.HELD)));

        assertThatThrownBy(() -> walletManager.hold(MEMBER_ID, POT_ID, 7_000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED);
        verify(walletRepository, never()).findByMemberIdForUpdate(any());
    }

    // ===================================================================== refundHold

    @Test
    @DisplayName("refundHold: 예치금 7,000 전액 환불 — 잔액 3,000 → 10,000, 원장 FARE_REFUND +7,000, 정산 REFUNDED")
    void refundHold_success() {
        Wallet wallet = wallet(3_000, 7_000);
        Settlement held = settlement(7_000, SettlementStatus.HELD);
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.of(held));
        when(walletRepository.findByMemberIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(wallet));

        walletManager.refundHold(MEMBER_ID, POT_ID);

        assertThat(wallet.getBalance()).isEqualTo(10_000);
        assertThat(wallet.getHeldBalance()).isZero();
        assertThat(held.getStatus()).isEqualTo(SettlementStatus.REFUNDED);

        PointHistory history = savedHistory();
        assertThat(history.getType()).isEqualTo(PointType.FARE_REFUND);
        assertThat(history.getAmount()).isEqualTo(7_000);
        assertThat(history.getBalanceAfter()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("refundHold: 이미 환불된 행 — 아무것도 안 함 (이중 환불 방지)")
    void refundHold_alreadyRefunded() {
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID))
                .thenReturn(Optional.of(settlement(7_000, SettlementStatus.REFUNDED)));

        walletManager.refundHold(MEMBER_ID, POT_ID);

        verify(walletRepository, never()).findByMemberIdForUpdate(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("refundHold: 정산 행이 없음 — 아무것도 안 함")
    void refundHold_noSettlement() {
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.empty());

        walletManager.refundHold(MEMBER_ID, POT_ID);

        verify(walletRepository, never()).findByMemberIdForUpdate(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("hold → refundHold 연속: 지갑이 원래대로 돌아온다")
    void holdThenRefund_restoresWallet() {
        Wallet wallet = wallet(10_000, 0);
        when(walletRepository.findByMemberIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(wallet));
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.empty());

        walletManager.hold(MEMBER_ID, POT_ID, 7_000);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        when(settlementRepository.findByPotIdAndMemberId(POT_ID, MEMBER_ID)).thenReturn(Optional.of(captor.getValue()));

        walletManager.refundHold(MEMBER_ID, POT_ID);

        assertThat(wallet.getBalance()).isEqualTo(10_000);
        assertThat(wallet.getHeldBalance()).isZero();
    }
}
