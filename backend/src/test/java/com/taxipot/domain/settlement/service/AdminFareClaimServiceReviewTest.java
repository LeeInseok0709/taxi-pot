package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.settlement.dto.FareClaimReviewRequest;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.FareClaimRepository;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PointType;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.file.FileStorage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 운영자 실요금 청구 승인·반려 (PATCH /api/admin/pots/{potId}/fare-claim) 단위 테스트.
 * <p>WalletLedger 는 진짜 객체, 지갑·정산 행도 진짜 엔티티로 만들어서 명세 예시 숫자가 그대로 나오는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminFareClaimServiceReviewTest {

    private static final Long POT_ID = 100L;
    private static final Long HOST = 1L;
    private static final Long GUEST_RICH = 2L;   // 잔액 넉넉
    private static final Long GUEST_POOR = 3L;   // 잔액 0원 → 미수금
    private static final Long ADMIN = 9L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 20, 0);

    @Mock FareClaimRepository fareClaimRepository;
    @Mock TaxipotRepository taxipotRepository;
    @Mock FareClaimResponseFactory fareClaimResponseFactory;
    @Mock FileStorage fileStorage;
    @Mock TaxipotMemberRepository taxipotMemberRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock MemberWalletLocker memberWalletLocker;
    @Mock NotificationSender notificationSender;
    @Mock PotRealtimePublisher potRealtimePublisher;
    @Mock AppProperties props;
    @Mock TimeProvider timeProvider;
    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    AdminFareClaimService service;
    Taxipot pot;
    Map<Long, Wallet> wallets = new LinkedHashMap<>();
    Map<Long, User> users = new LinkedHashMap<>();
    Map<Long, Settlement> settlements = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        WalletLedger ledger = new WalletLedger(walletRepository, pointHistoryRepository, props);
        service = new AdminFareClaimService(fareClaimRepository, taxipotRepository, fareClaimResponseFactory, fileStorage,
                taxipotMemberRepository, settlementRepository, memberWalletLocker, ledger, notificationSender,
                potRealtimePublisher, props, timeProvider);

        when(props.pot()).thenReturn(new AppProperties.Pot(15, 24, 5, 10, 30, 3, 3, 12, 10, 1_000, 15, 1_000, 500_000));
        when(props.debt()).thenReturn(new AppProperties.Debt(3));
        when(props.wallet()).thenReturn(new AppProperties.Wallet(500_000, 10_000, 50_000));
        when(timeProvider.now()).thenReturn(NOW);
        when(fareClaimResponseFactory.of(any(), anyString())).thenReturn(null);

        // 명세 예시: 실요금 25,000원, 3명 탑승, 각자 예치 5,500원
        pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(POT_ID);
        when(pot.getHostId()).thenReturn(HOST);
        when(pot.getTitle()).thenReturn("두정역 → 본관");
        when(pot.getStatus()).thenReturn(PotStatus.RUNNING);
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.PENDING);
        when(pot.getActualFare()).thenReturn(25_000);
        when(taxipotRepository.findByIdForUpdate(POT_ID)).thenReturn(Optional.of(pot));

        member(HOST, 0, 0);
        member(GUEST_RICH, 10_000, 1);   // 신고 이력 1회 → clean_streak 대상
        member(GUEST_POOR, 0, 0);

        List<TaxipotMember> boarded = wallets.keySet().stream().map(id -> {
            TaxipotMember m = mock(TaxipotMember.class);
            when(m.getMemberId()).thenReturn(id);
            return m;
        }).toList();
        when(taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(eq(POT_ID), anyCollection())).thenReturn(boarded);
        when(settlementRepository.findAllByPotId(POT_ID)).thenReturn(List.copyOf(settlements.values()));

        Map<Long, LockedMember> locked = new LinkedHashMap<>();
        wallets.forEach((id, w) -> locked.put(id, new LockedMember(users.get(id), w)));
        when(memberWalletLocker.lockAll(anyCollection())).thenReturn(locked);
    }

    /** 탑승자 한 명: 예치 5,500원이 걸린 지갑(사용 가능 잔액 balance) + HELD 정산 행 */
    private void member(Long id, int balance, int reportCount) {
        Wallet w = Wallet.open(id);
        w.deposit(balance + 5_500);
        w.hold(5_500);
        wallets.put(id, w);
        settlements.put(id, Settlement.hold(POT_ID, id, 5_500));
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getReportCount()).thenReturn(reportCount);
        users.put(id, u);
    }

    private List<PointHistory> histories(Long memberId) {
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues().stream().filter(h -> h.getMemberId().equals(memberId)).toList();
    }

    private FareClaimResponse approve() {
        return service.reviewFareClaim(ADMIN, POT_ID, new FareClaimReviewRequest(FareClaimStatus.APPROVED, null));
    }

    // ===================================================================== 승인 → 정산

    @Test
    @DisplayName("방장: +25,000 +5,500 −8,333 −1,000 → 21,167 (명세 예시와 같음), 원장 순서 HOST_PAYOUT→FARE_REFUND→FARE_PAY→FEE")
    void approve_host() {
        approve();

        assertThat(wallets.get(HOST).getBalance()).isEqualTo(21_167);
        assertThat(wallets.get(HOST).getHeldBalance()).isZero();
        assertThat(histories(HOST)).extracting(PointHistory::getType)
                .containsExactly(PointType.HOST_PAYOUT, PointType.FARE_REFUND, PointType.FARE_PAY, PointType.FEE);
        assertThat(histories(HOST)).extracting(PointHistory::getAmount)
                .containsExactly(25_000, 5_500, -8_333, -1_000);
    }

    @Test
    @DisplayName("잔액 넉넉한 동승자: 10,000 + 5,500 − 8,333 − 1,000 = 6,167, 미수금 없음, diff = −3,833")
    void approve_richGuest() {
        approve();

        assertThat(wallets.get(GUEST_RICH).getBalance()).isEqualTo(6_167);
        Settlement s = settlements.get(GUEST_RICH);
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(s.getFareShare()).isEqualTo(8_333);
        assertThat(s.getFeeAmount()).isEqualTo(1_000);
        assertThat(s.getDiffAmount()).isEqualTo(5_500 - 9_333);
        assertThat(s.getUncoveredAmount()).isZero();
        assertThat(s.getDebtStatus()).isEqualTo(DebtStatus.NONE);
    }

    @Test
    @DisplayName("잔액 0원 동승자: 명세 예시 그대로 FARE_REFUND +5,500 → FARE_PAY −5,500, FEE 없음, 미수금 3,833원(기한 3일) + 알림")
    void approve_poorGuest_debt() {
        approve();

        assertThat(wallets.get(GUEST_POOR).getBalance()).isZero();
        assertThat(histories(GUEST_POOR)).extracting(PointHistory::getType)
                .containsExactly(PointType.FARE_REFUND, PointType.FARE_PAY);
        assertThat(histories(GUEST_POOR)).extracting(PointHistory::getAmount)
                .containsExactly(5_500, -5_500);

        Settlement s = settlements.get(GUEST_POOR);
        assertThat(s.getUncoveredAmount()).isEqualTo(3_833);   // 분담금 2,833 + 수수료 1,000
        assertThat(s.getDebtStatus()).isEqualTo(DebtStatus.UNPAID);
        assertThat(s.getDebtAmount()).isEqualTo(3_833);
        assertThat(s.getDebtDueAt()).isEqualTo(NOW.plusDays(3));
        verify(notificationSender).send(eq(GUEST_POOR), eq(POT_ID), eq(NotificationType.PAYMENT), eq("미수금 발생"), anyString());
        verify(notificationSender, never()).send(eq(GUEST_RICH), any(), any(), eq("미수금 발생"), anyString());
    }

    @Test
    @DisplayName("방 종료 처리 + 신고 이력 있는 사람만 clean_streak + 탑승자 전원 정산 완료·평가 요청 알림")
    void approve_afterEffects() {
        approve();

        verify(pot).approveFareClaim(25_000, NOW);
        verify(users.get(GUEST_RICH)).increaseCleanStreak();
        verify(users.get(HOST), never()).increaseCleanStreak();
        verify(users.get(GUEST_POOR), never()).increaseCleanStreak();
        verify(notificationSender).sendAll(eq(List.of(HOST, GUEST_RICH, GUEST_POOR)), eq(POT_ID),
                eq(NotificationType.PAYMENT), eq("정산 완료"), anyString());
        verify(notificationSender).sendAll(eq(List.of(HOST, GUEST_RICH, GUEST_POOR)), eq(POT_ID),
                eq(NotificationType.RATING_REQUEST), anyString(), anyString());
        verify(potRealtimePublisher).potUpdated(POT_ID);
    }

    @Test
    @DisplayName("돈 보존: 방장이 받은 25,000 = 탑승자 분담금 합 24,999 + 끝수 1 (미수금은 운영자가 대신 낸 셈)")
    void approve_moneyConservation() {
        approve();

        int paidShares = settlements.values().stream().mapToInt(Settlement::getFareShare).sum();
        assertThat(paidShares + 1).isEqualTo(25_000);
    }

    @Test
    @DisplayName("PENDING이 아니면 409 FARE_CLAIM_NOT_PENDING, 돈 이동 없음")
    void approve_notPending() {
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.APPROVED);

        assertThatThrownBy(this::approve)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FARE_CLAIM_NOT_PENDING);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("운행 중(RUNNING)이 아닌 방이면 409 INVALID_POT_STATUS")
    void approve_notRunning() {
        when(pot.getStatus()).thenReturn(PotStatus.CANCELED);

        assertThatThrownBy(this::approve)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_POT_STATUS);
    }

    // ===================================================================== 반려

    @Test
    @DisplayName("반려: 사유 저장, 방장에게 알림, 돈 이동·회원 락 없음")
    void reject() {
        service.reviewFareClaim(ADMIN, POT_ID, new FareClaimReviewRequest(FareClaimStatus.REJECTED, "  영수증 금액이 다릅니다 "));

        verify(pot).rejectFareClaim("영수증 금액이 다릅니다", NOW);
        verify(notificationSender).send(eq(HOST), eq(POT_ID), eq(NotificationType.PAYMENT), eq("정산 청구 반려"), anyString());
        verify(memberWalletLocker, never()).lockAll(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("반려 사유가 비어 있으면 400 REJECT_REASON_REQUIRED")
    void reject_noReason() {
        assertThatThrownBy(() -> service.reviewFareClaim(ADMIN, POT_ID, new FareClaimReviewRequest(FareClaimStatus.REJECTED, " ")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REJECT_REASON_REQUIRED);
        verify(pot, never()).rejectFareClaim(any(), any());
    }

    @Test
    @DisplayName("decision이 APPROVED·REJECTED가 아니면 400 INVALID_INPUT")
    void invalidDecision() {
        assertThatThrownBy(() -> service.reviewFareClaim(ADMIN, POT_ID, new FareClaimReviewRequest(FareClaimStatus.PENDING, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
