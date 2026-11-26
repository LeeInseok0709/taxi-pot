package com.taxipot.domain.rating.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * RatingResponse — API 명세서 3.9.0 components.schemas.RatingResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record RatingResponse(
        Long id,
        Long potId,
        Long reviewerId,
        Long revieweeId,
        Integer noshowScore,
        Integer mannerScore,
        Integer communicationScore,
        Integer paymentScore,
        // DB 생성 컬럼 값
        Double avgScore,
        // nullable
        String badReason,
        // true = 정산 24시간 후 서버가 넣은 기본 평가(4항목 ★3), false = 회원 제출 (3.3.0)
        @JsonProperty("isDefault") Boolean isDefault,
        OffsetDateTime createdAt
) {
}
