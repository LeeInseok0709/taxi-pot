package com.taxipot.domain.settlement.service;

import com.taxipot.domain.settlement.dto.BatchStatusRequest;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminBatchStatusService {

    /**
     * <b>POST /api/admin/pots/batch-status</b> — 장애 복구 후 상태 일괄 입력 (선택 기능)
     * <p>
     * 서버 장애 동안 오프라인으로 처리된 매칭·탑승 결과를 복구 후 일괄 반영 (요구사항 0.2 신뢰도).
     * 한 요청 안의 항목은 하나의 트랜잭션으로 처리.
     * - potStatus = COMPLETED이면 actualFare 필수 (DB CHECK chk_taxipot_completed_fare) → 청구 승인과 같은 로직(방장 정산금·미수금 포함)으로 정산까지 처리,
     *   fare_claim_status = APPROVED, fare_reviewed_at = now (DB CHECK chk_taxipot_completed_claim)
     * - 24시간 내 미청구(OVERDUE)·장기 미처리 방 정리에도 사용 (3.5.0): 증빙을 확인해 COMPLETED(정산) 또는 CANCELED(전액 환불)로 반영
     * - 영수증 사진 없이 처리 가능 (receipt_img_path = NULL, 운영자가 오프라인 증빙 확인)
     * - CANCELED로 반영하면 HELD 정산은 전액 환불(REFUNDED)
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / INVALID_DATETIME_FORMAT
     * - 403: FORBIDDEN
     */
    @Transactional
    public void applyBatchStatus(Long adminId, BatchStatusRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/admin/pots/batch-status");
    }
}
