package com.taxipot.domain.verification.dto;

import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.ReviewStatus;

import java.time.OffsetDateTime;

/**
 * AdminVerificationResponse — API 명세서 3.9.0 components.schemas.AdminVerificationResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AdminVerificationResponse(
        Long id,
        Long memberId,
        // 가입 시 입력한 이름 (캡처본과 대조)
        String memberName,
        Gender memberGender,
        String university,
        // USERS.student_no
        String registeredStudentNo,
        // STUDENT_VERIFICATION.student_no
        String submittedStudentNo,
        String email,
        EmailStatus emailStatus,
        // nullable / 캡처 이미지 조회 API 경로 `/api/admin/verifications/{id}/capture` (운영자 토큰 필요). 미제출이면 null. DB 파일 …
        String captureImgUrl,
        ReviewStatus reviewStatus,
        // nullable
        String rejectReason,
        // nullable
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
}
