package com.taxipot.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UserPublicResponse — API 명세서 3.9.0 components.schemas.UserPublicResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record UserPublicResponse(
        Long id,
        String nickname,
        String university,
        Double ratingAvg,
        // 탈퇴 회원 여부 (탈퇴 14일 후 닉네임은 '탈퇴회원_{id}')
        @JsonProperty("isWithdrawn") Boolean isWithdrawn
) {
}
