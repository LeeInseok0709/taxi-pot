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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 담당: 지갑·정산 — 스케줄러 작업 8·9·10 (TaxipotScheduler 가 주기적으로 호출)
 * <p>대상은 번호만 먼저 뽑고, 한 건씩 행을 잠근 뒤 조건을 다시 확인한다.
 * 같은 순간에 사용자가 납부·청구를 하면 그쪽 결과를 덮어쓰지 않게 하기 위해서다.
 */
@Service
@RequiredArgsConstructor
public class SettlementJobService {

    private final SettlementRepository settlementRepository;
    private final FareClaimRepository fareClaimRepository;
    private final TaxipotRepository taxipotRepository;
    private final FileStorage fileStorage;
    private final NotificationSender notificationSender;
    private final AppProperties props;
    private final TimeProvider timeProvider;

    /** 8. 미수금 연체 표시 (10분): debt_status = UNPAID 이고 debt_due_at &lt;= now → OVERDUE, PAYMENT 알림. debt_amount 는 그대로 */
    @Transactional
    public void markOverdueDebts() {
        LocalDateTime now = timeProvider.now();
        for (Long settlementId : settlementRepository.findIdsByDebtStatusAndDueAtUpTo(DebtStatus.UNPAID, now)) {
            Settlement settlement = settlementRepository.findByIdForUpdate(settlementId).orElse(null);
            // 잠그는 사이 납부했으면 건너뜀
            if (settlement == null || settlement.getDebtStatus() != DebtStatus.UNPAID
                    || settlement.getDebtDueAt() == null || settlement.getDebtDueAt().isAfter(now)) {
                continue;
            }
            settlement.markDebtOverdue();
            notificationSender.send(settlement.getMemberId(), settlement.getPotId(), NotificationType.PAYMENT, "미수금 연체",
                    "미수금 %,d원의 납부 기한이 지났습니다. 납부 전까지 이용이 제한되니 지금 납부해 주세요."
                            .formatted(settlement.getDebtAmount()));
        }
    }

    /**
     * 9. 실요금 청구 기한 경과 (10분): RUNNING, fare_claim_status = NONE, started_at + 24시간 &lt;= now → OVERDUE,
     * 운영자 SYSTEM 알림(NotificationSender.sendToAdmins) + 방장 PAYMENT 알림
     */
    @Transactional
    public void markOverdueFareClaims() {
        long hours = props.fareClaim().deadlineHours();
        LocalDateTime threshold = timeProvider.now().minusHours(hours);
        for (Long potId : fareClaimRepository.findClaimOverdueTargetIds(PotStatus.RUNNING, FareClaimStatus.NONE, threshold)) {
            Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElse(null);
            // 잠그는 사이 방장이 청구했으면 건너뜀
            if (pot == null || pot.getStatus() != PotStatus.RUNNING || pot.getFareClaimStatus() != FareClaimStatus.NONE
                    || pot.getStartedAt() == null || pot.getStartedAt().isAfter(threshold)) {
                continue;
            }
            pot.markFareClaimOverdue();
            notificationSender.sendToAdmins(potId, NotificationType.SYSTEM, "정산 청구 없음",
                    "'%s' 방이 탑승 후 %d시간이 지나도록 실요금 청구가 없습니다. 장애 복구 입력으로 정리해 주세요."
                            .formatted(pot.getTitle(), hours));
            notificationSender.send(pot.getHostId(), potId, NotificationType.PAYMENT, "실요금 청구 기한 경과",
                    "탑승 후 %d시간이 지났어요. 영수증 사진으로 실요금을 청구해 주세요.".formatted(hours));
        }
    }

    /**
     * 10. 영수증 사진 파기 (일 1회): receipt_img_path 가 있는 방 중 ① COMPLETED 이고 fare_reviewed_at + 90일 &lt;= now,
     * ② CANCELED 이고 updated_at + 90일 &lt;= now → FileStorage.delete 성공 시에만 Taxipot.clearReceipt()
     */
    @Transactional
    public void purgeOldReceipts() {
        LocalDateTime threshold = timeProvider.now().minusDays(props.receipt().retentionDays());
        for (Long potId : fareClaimRepository.findReceiptPurgeTargetIds(PotStatus.COMPLETED, PotStatus.CANCELED, threshold)) {
            Taxipot pot = taxipotRepository.findById(potId).orElse(null);
            if (pot == null || pot.getReceiptImgPath() == null) {
                continue;
            }
            // 파일 삭제에 실패하면 경로를 남겨 두고 다음 날 다시 시도
            if (fileStorage.delete(pot.getReceiptImgPath())) {
                pot.clearReceipt();
            }
        }
    }
}
