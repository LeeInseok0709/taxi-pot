package com.taxipot.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;

import java.time.OffsetDateTime;

/**
 * MyProfileResponse — API 명세서 3.9.0 components.schemas.MyProfileResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
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
        @JsonProperty("isVerified") Boolean isVerified,
        Double ratingAvg,
        Integer noShowCount,
        // 승인된 신고 누적 (무신고 차감 반영 후). 3회 3일 / 6회 7일 / 10회 영구 정지
        Integer reportCount,
        // 신고 없이 탑승 완료한 연속 횟수 (3 도달 시 reportCount −1 후 0)
        Integer cleanStreak,
        UserRole role,
        UserStatus status,
        // nullable / SUSPENDED일 때만 값 존재, NULL이면 영구 정지(운영자만 해제). 정지 사유는 제공하지 않음
        OffsetDateTime suspendedUntil,
        Boolean agreeMarketing,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        // nullable / 다음 정지까지 남은 신고 횟수 (화면 안내용, 영구 정지면 null)
        Integer nextSuspensionIn
) {
}
