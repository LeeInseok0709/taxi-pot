package com.taxipot.settlement.service;

import com.taxipot.global.file.StoredImage;
import com.taxipot.settlement.dto.FareClaimResponse;
import com.taxipot.settlement.dto.PotSettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * FareClaimController 전용 API 서비스. 담당: 효우
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareClaimService {

    /**
     * POST /api/pots/{potId}/fare-claim — 실요금 청구 (방장 전용, 3.5.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 하차 후 방장이 결제내역(영수증) 사진과 실제 요금으로 정산을 청구한다. 정산(돈 이동)은 운영자가 승인할 때 실행된다 (3.5.0, 기존 즉시 정산 API 대체).
     * `multipart/form-data` — 파트 `actualFare`(텍스트, 정수) + `receiptImage`(jpg/png 파일)
     * - Kotlin: `@Multipart` + `@Part("actualFare") actualFare: RequestBody`(`"19300".toRequestBody("text/plain".toMediaType())`) + `@Part receiptImage: MultipartBody.Part`
     * - Spring: `@RequestParam("actualFare") Integer actualFare, @RequestPart("receiptImage") MultipartFile receiptImage`
     * - 사진이 없거나 비어 있으면 400 RECEIPT_REQUIRED, 형식 오류는 400 INVALID_INPUT
     * - 가능 조건: TAXIPOT status = RUNNING이고 fare_claim_status가 NONE·REJECTED·OVERDUE
     *   - PENDING이면 409 FARE_CLAIM_IN_REVIEW, APPROVED면 409 ALREADY_SETTLED
     * - 처리: 영수증 파일 저장(`{app.upload-dir}/receipts/{potId}/{UUID}`, 재청구면 이전 파일 삭제), actual_fare·receipt_img_path 저장,
     *   fare_claim_status = PENDING, fare_claimed_at = now, fare_reject_reason = NULL
     * - 운영자(role = ADMIN)에게 SYSTEM 알림('정산 청구 확인 요청'), 탑승자에게 STOMP `POT_UPDATED`
     * - 청구 접수 후 탑승자는 '진행 중인 다른 방'에서 제외되어 새 방을 이용할 수 있다
     */
    @Transactional
    public FareClaimResponse createFareClaim(Long userId, Long potId, Integer actualFare, MultipartFile receiptImage) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/pots/{potId}/fare-claim");
    }

    /**
     * GET /api/pots/{potId}/settlement — 택시팟 정산 내역 조회 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 해당 방 참여자만 조회 가능. 참여자별 예치액·분담금·수수료·차액·미수금 + 방장 정산금·영수증 URL 반환. 운영자 승인 전에는 정산 값이 null (3.5.0)
     */
    public PotSettlementResponse getPotSettlement(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/pots/{potId}/settlement");
    }

    /**
     * GET /api/pots/{potId}/receipt — 결제내역(영수증) 사진 조회 (3.3.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 해당 방 탑승자(BOARDED)만 조회 가능 (운영자는 `/api/admin/pots/{potId}/receipt`). 이미지 바이너리 반환, Bearer 토큰 필요
     * (공통 규칙 '이미지 조회 API' 참고). 영수증이 없거나 보관 기간(정산 승인 후 90일)이 지나 파기됐으면 404 (3.6.0)
     */
    public StoredImage getReceiptImage(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/pots/{potId}/receipt");
    }
}
