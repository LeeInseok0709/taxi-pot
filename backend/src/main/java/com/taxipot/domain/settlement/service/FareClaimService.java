package com.taxipot.domain.settlement.service;

import com.taxipot.domain.settlement.dto.FareClaimRequest;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
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
public class FareClaimService {

    /**
     * <b>POST /api/pots/{potId}/fare-claim</b> — 실요금 청구 (방장 전용, 3.5.0)
     * <p>
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
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT(금액 범위·이미지 형식) / RECEIPT_REQUIRED
     * - 403: NOT_POT_HOST
     * - 409: INVALID_POT_STATUS / FARE_CLAIM_IN_REVIEW / ALREADY_SETTLED
     */
    @Transactional
    public FareClaimResponse claimFare(Long userId, Long potId, FareClaimRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/fare-claim");
    }
}
