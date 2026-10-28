package com.taxipot.domain.rating.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.global.code.PotMemberRole;

/**
 * RatingTargetResponse — API 명세서 3.9.0 components.schemas.RatingTargetResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record RatingTargetResponse(
        Long memberId,
        String nickname,
        PotMemberRole role,
        // 내가 이미 평가했는지 여부 (자동 기본 평가가 들어갔어도 true)
        @JsonProperty("isRated") Boolean isRated
) {
}
