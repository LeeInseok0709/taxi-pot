package com.taxipot.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;

import java.time.OffsetDateTime;

/**
 * AdminUserResponse — API 명세서 3.9.0 components.schemas.AdminUserResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AdminUserResponse(
        Long id,
        // nullable
        String email,
        // nullable
        String name,
        String nickname,
        String university,
        // nullable
        String studentNo,
        @JsonProperty("isVerified") Boolean isVerified,
        Double ratingAvg,
        Integer noShowCount,
        Integer reportCount,
        UserRole role,
        UserStatus status,
        // nullable
        OffsetDateTime suspendedUntil,
        // nullable
        OffsetDateTime withdrawnAt,
        OffsetDateTime createdAt
) {
}
