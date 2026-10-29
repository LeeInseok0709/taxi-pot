package com.taxipot.domain.verification.dto;

import com.taxipot.global.code.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * VerificationReviewRequest — API 명세서 3.9.0 components.schemas.VerificationReviewRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record VerificationReviewRequest(
        // 허용 값: APPROVED, REJECTED (그 외는 서비스에서 INVALID_INPUT)
        @NotNull ReviewStatus reviewStatus,
        // 선택 / REJECTED일 때 필수
        @Size(max = 255) String rejectReason
) {
}
