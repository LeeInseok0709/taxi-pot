package com.taxipot.user.dto;

import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;
import com.taxipot.pot.dto.MyPotHistoryResponse;
import com.taxipot.report.dto.AdminReportResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 명세 3.9.0 스키마 AdminUserDetailResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record AdminUserDetailResponse(
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
        OffsetDateTime createdAt,
        Integer cleanStreak,
        Integer walletBalance,
        Integer heldBalance,
        /** 남은 미수금 합계 (3.3.0) */
        Integer debtAmount,
        /** 캡처본 제출·심사 이력 (탈퇴 14일 후 삭제) */
        List<AdminVerificationResponse> verifications,
        List<AdminReportResponse> receivedReports,
        /** null 가능 */
        String phone,
        /** null 가능 */
        Gender gender,
        /** 최근 이용 기록 10건 (전체는 /api/admin/users/{userId}/pots) */
        List<MyPotHistoryResponse> potHistory
) {}
