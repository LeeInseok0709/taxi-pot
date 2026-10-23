package com.taxipot.user.dto;

import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.Gender;
import com.taxipot.global.code.ReviewStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 AdminVerificationResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record AdminVerificationResponse(
        Long id,
        Long memberId,
        /** 가입 시 입력한 이름 (캡처본과 대조) */
        String memberName,
        Gender memberGender,
        String university,
        /** USERS.student_no */
        String registeredStudentNo,
        /** STUDENT_VERIFICATION.student_no */
        String submittedStudentNo,
        String email,
        EmailStatus emailStatus,
        /** 캡처 이미지 조회 API 경로 `/api/admin/verifications/{id}/capture` (운영자 토큰 필요). 미제출이면 null. DB 파일 경로는 노출하지 않음 (3.3.0); null 가능 */
        String captureImgUrl,
        ReviewStatus reviewStatus,
        /** null 가능 */
        String rejectReason,
        /** null 가능 */
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {}
