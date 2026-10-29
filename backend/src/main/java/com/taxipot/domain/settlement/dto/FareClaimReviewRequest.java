package com.taxipot.domain.settlement.dto;

import com.taxipot.global.code.FareClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * FareClaimReviewRequest — API 명세서 3.9.0 components.schemas.FareClaimReviewRequest
 * <p>실요금 청구 승인·반려 (운영자, 3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record FareClaimReviewRequest(
        // 허용 값: APPROVED, REJECTED (그 외는 서비스에서 INVALID_INPUT)
        @NotNull FareClaimStatus decision,
        // 선택 / null 허용 / REJECTED면 필수
        @Size(max = 255) String rejectReason
) {
}
