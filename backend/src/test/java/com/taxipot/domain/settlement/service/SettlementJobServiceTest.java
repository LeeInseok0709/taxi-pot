package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.FareClaimRepository;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 스케줄러 8·9·10 (미수금 연체, 청구 기한 경과, 영수증 파기) 단위 테스트 — DB·파일 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementJobServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 21, 0);

    @Mock SettlementRepository settlementRepository;
    @Mock FareClaimRepository fareClaimRepository;
    @Mock TaxipotRepository taxipotRepository;
    @Mock FileStorage fileStorage;
    @Mock NotificationSender notificationSender;
    @Mock AppProperties props;
    @Mock TimeProvider timeProvider;

    SettlementJobService service;

    @BeforeEach
    void setUp() {
        service = new SettlementJobService(settlementRepository, fareClaimRepository, taxipotRepository, fileStorage,
                notificationSender, props, timeProvider);
        when(timeProvider.now()).thenReturn(NOW);
        when(props.fareClaim()).thenReturn(new AppProperties.FareClaim(24));
        when(props.receipt()).thenReturn(new AppProperties.Receipt(90));
    }

    private Settlement debt(Long memberId, int amount, LocalDateTime dueAt) {
        Settlement s = Settlement.hold(100L, memberId, 5_500);
        s.settle(9_333, 1_000, -3_833, amount, NOW.minusDays(4), dueAt);
        return s;
    }

    // ===================================================================== 8. 미수금 연체

    @Test
    @DisplayName("8. 기한 지난 미납 → OVERDUE + 알림, 금액은 그대로 / 잠그는 사이 납부한 건은 건너뜀")
    void markOverdueDebts() {
        Settlement overdue = debt(2L, 3_833, NOW.minusHours(1));
        Settlement paidMeanwhile = debt(3L, 2_000, NOW.minusHours(1));
        paidMeanwhile.payDebt(2_000, NOW.minusMinutes(1));          // 사용자가 방금 다 갚음
        when(settlementRepository.findIdsByDebtStatusAndDueAtUpTo(DebtStatus.UNPAID, NOW)).thenReturn(List.of(5L, 6L));
        when(settlementRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(overdue));
        when(settlementRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(paidMeanwhile));

        service.markOverdueDebts();

        assertThat(overdue.getDebtStatus()).isEqualTo(DebtStatus.OVERDUE);
        assertThat(overdue.getDebtAmount()).isEqualTo(3_833);      // 10배 같은 가산 없음
        assertThat(paidMeanwhile.getDebtStatus()).isEqualTo(DebtStatus.PAID);
        verify(notificationSender).send(eq(2L), eq(100L), eq(NotificationType.PAYMENT), eq("미수금 연체"), anyString());
        verify(notificationSender, never()).send(eq(3L), any(), any(), anyString(), anyString());
    }

    // ===================================================================== 9. 청구 기한 경과

    private Taxipot pot(Long id, FareClaimStatus claim, LocalDateTime startedAt) {
        Taxipot pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(id);
        when(pot.getHostId()).thenReturn(1L);
        when(pot.getTitle()).thenReturn("두정역 → 본관");
        when(pot.getStatus()).thenReturn(PotStatus.RUNNING);
        when(pot.getFareClaimStatus()).thenReturn(claim);
        when(pot.getStartedAt()).thenReturn(startedAt);
        return pot;
    }

    @Test
    @DisplayName("9. 탑승 24시간 지나도록 청구 없음 → OVERDUE + 운영자·방장 알림 / 그 사이 청구한 방은 건너뜀")
    void markOverdueFareClaims() {
        Taxipot late = pot(100L, FareClaimStatus.NONE, NOW.minusHours(25));
        Taxipot claimedMeanwhile = pot(101L, FareClaimStatus.PENDING, NOW.minusHours(25));
        when(fareClaimRepository.findClaimOverdueTargetIds(PotStatus.RUNNING, FareClaimStatus.NONE, NOW.minusHours(24)))
                .thenReturn(List.of(100L, 101L));
        when(taxipotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(late));
        when(taxipotRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(claimedMeanwhile));

        service.markOverdueFareClaims();

        verify(late).markFareClaimOverdue();
        verify(claimedMeanwhile, never()).markFareClaimOverdue();
        verify(notificationSender).sendToAdmins(eq(100L), eq(NotificationType.SYSTEM), eq("정산 청구 없음"), anyString());
        verify(notificationSender).send(eq(1L), eq(100L), eq(NotificationType.PAYMENT), eq("실요금 청구 기한 경과"), anyString());
    }

    // ===================================================================== 10. 영수증 파기

    @Test
    @DisplayName("10. 보관 90일 지난 영수증: 파일 삭제에 성공한 방만 경로를 비운다 (실패하면 다음 날 재시도)")
    void purgeOldReceipts() {
        Taxipot ok = mock(Taxipot.class);
        when(ok.getReceiptImgPath()).thenReturn("receipts/100/a.jpg");
        Taxipot fail = mock(Taxipot.class);
        when(fail.getReceiptImgPath()).thenReturn("receipts/101/b.jpg");
        when(fareClaimRepository.findReceiptPurgeTargetIds(PotStatus.COMPLETED, PotStatus.CANCELED, NOW.minusDays(90)))
                .thenReturn(List.of(100L, 101L));
        when(taxipotRepository.findById(100L)).thenReturn(Optional.of(ok));
        when(taxipotRepository.findById(101L)).thenReturn(Optional.of(fail));
        when(fileStorage.delete("receipts/100/a.jpg")).thenReturn(true);
        when(fileStorage.delete("receipts/101/b.jpg")).thenReturn(false);

        service.purgeOldReceipts();

        verify(ok).clearReceipt();
        verify(fail, never()).clearReceipt();
    }
}
