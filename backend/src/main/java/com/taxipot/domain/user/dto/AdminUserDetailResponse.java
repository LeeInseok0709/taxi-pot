package com.taxipot.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.verification.dto.AdminVerificationResponse;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AdminUserDetailResponse — API 명세서 3.9.0 components.schemas.AdminUserDetailResponse
 * <p><b>손으로 채운 파일 (인석)</b>: 명세가 allOf(AdminUserResponse + 추가 필드) 구조라 tools/gen_dto.py 가
 * 빈 record 를 만들었다. allOf 를 펼친 순서대로 AdminUserResponse 의 필드 → 추가 필드를 그대로 나열했다.
 * gen_dto.py 가 allOf 를 지원하게 되면 재생성해도 같은 모양이어야 한다.
 */
public record AdminUserDetailResponse(
        // ---- AdminUserResponse 와 같은 필드 ----
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
        OffsetDateTime createdAt,
        // ---- 상세에만 있는 필드 ----
        Integer cleanStreak,
        Integer walletBalance,
        Integer heldBalance,
        // 남은 미수금 합계 (3.3.0)
        Integer debtAmount,
        // 캡처본 제출·심사 이력 (탈퇴 14일 후 삭제)
        List<AdminVerificationResponse> verifications,
        List<AdminReportResponse> receivedReports,
        // nullable
        String phone,
        // nullable
        Gender gender,
        // 최근 이용 기록 10건 (전체는 /api/admin/users/{userId}/pots)
        List<MyPotHistoryResponse> potHistory
) {
}
