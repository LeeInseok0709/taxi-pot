package com.taxipot.domain.settlement.service;

import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 담당: 지갑·정산 — 스케줄러 작업 8·9·10 */
@Service
@RequiredArgsConstructor
public class SettlementJobService {

    /** 8. 미수금 연체 표시 (10분): debt_status = UNPAID 이고 debt_due_at &lt;= now → OVERDUE, PAYMENT 알림. debt_amount 는 그대로 */
    public void markOverdueDebts() {
        throw new NotImplementedYetException("스케줄러 8 미수금 연체 표시");
    }

    /**
     * 9. 실요금 청구 기한 경과 (10분): RUNNING, fare_claim_status = NONE, started_at + 24시간 &lt;= now → OVERDUE,
     * 운영자 SYSTEM 알림(NotificationSender.sendToAdmins) + 방장 PAYMENT 알림
     */
    public void markOverdueFareClaims() {
        throw new NotImplementedYetException("스케줄러 9 실요금 청구 기한 경과");
    }

    /**
     * 10. 영수증 사진 파기 (일 1회): receipt_img_path 가 있는 방 중 ① COMPLETED 이고 fare_reviewed_at + 90일 &lt;= now,
     * ② CANCELED 이고 updated_at + 90일 &lt;= now → FileStorage.delete 성공 시에만 Taxipot.clearReceipt()
     */
    public void purgeOldReceipts() {
        throw new NotImplementedYetException("스케줄러 10 영수증 사진 파기");
    }
}
