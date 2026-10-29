package com.taxipot.domain.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * RatingItem — API 명세서 3.9.0 components.schemas.RatingItem
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record RatingItem(
        @NotNull Long revieweeId,
        // 시간 약속 별점
        @NotNull @Min(1) @Max(5) Integer noshowScore,
        // 언행·태도 별점
        @NotNull @Min(1) @Max(5) Integer mannerScore,
        // 연락 별점
        @NotNull @Min(1) @Max(5) Integer communicationScore,
        // 정산 별점
        @NotNull @Min(1) @Max(5) Integer paymentScore,
        // 선택 / 4항목 평균 3점 미만(별로였어요)일 때 필수
        @Size(max = 255) String badReason
) {
}
