package com.taxipot.user.dto;

import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 MyProfileResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record MyProfileResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String phone,
        Gender gender,
        String university,
        String studentNo,
        Boolean isVerified,
        Double ratingAvg,
        Integer noShowCount,
        /** 승인된 신고 누적 (무신고 차감 반영 후). 3회 3일 / 6회 7일 / 10회 영구 정지 */
        Integer reportCount,
        /** 신고 없이 탑승 완료한 연속 횟수 (3 도달 시 reportCount −1 후 0) */
        Integer cleanStreak,
        UserRole role,
        UserStatus status,
        /** SUSPENDED일 때만 값 존재, NULL이면 영구 정지(운영자만 해제). 정지 사유는 제공하지 않음; null 가능 */
        OffsetDateTime suspendedUntil,
        Boolean agreeMarketing,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        /** 다음 정지까지 남은 신고 횟수 (화면 안내용, 영구 정지면 null); null 가능 */
        Integer nextSuspensionIn
) {}
