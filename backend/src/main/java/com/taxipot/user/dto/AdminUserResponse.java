package com.taxipot.user.dto;

import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 AdminUserResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record AdminUserResponse(
        Long id,
        /** null 가능 */
        String email,
        /** null 가능 */
        String name,
        String nickname,
        String university,
        /** null 가능 */
        String studentNo,
        Boolean isVerified,
        Double ratingAvg,
        Integer noShowCount,
        Integer reportCount,
        UserRole role,
        UserStatus status,
        /** null 가능 */
        OffsetDateTime suspendedUntil,
        /** null 가능 */
        OffsetDateTime withdrawnAt,
        OffsetDateTime createdAt
) {}
