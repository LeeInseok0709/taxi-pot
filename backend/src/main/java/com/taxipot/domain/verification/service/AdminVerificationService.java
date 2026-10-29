package com.taxipot.domain.verification.service;

import com.taxipot.domain.verification.dto.AdminVerificationPageResponse;
import com.taxipot.domain.verification.dto.AdminVerificationResponse;
import com.taxipot.domain.verification.dto.VerificationReviewRequest;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.error.NotImplementedYetException;
import com.taxipot.global.file.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    /**
     * <b>GET /api/admin/verifications</b> — 학생 인증 심사 목록 조회
     * <p>
     * reviewStatus 미지정 시 PENDING(심사 대기)만 반환. NOT_SUBMITTED(캡처 미제출)는 지정할 때만 조회 (3.3.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public AdminVerificationPageResponse getVerifications(Long adminId, ReviewStatus reviewStatus, int page, int size) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/verifications");
    }

    /**
     * <b>GET /api/admin/verifications/{verificationId}</b> — 학생 인증 상세 조회 (캡처본 열람)
     * <p>
     * 가입 정보(이름·성별·대학교·학번)와 캡처 이미지 주소(captureImgUrl)를 함께 반환해 대조할 수 있게 한다.
     * 이미지 자체는 captureImgUrl(`/api/admin/verifications/{id}/capture`)을 운영자 토큰으로 요청해서 받는다
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AdminVerificationResponse getVerification(Long adminId, Long verificationId) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/verifications/{verificationId}");
    }

    /**
     * <b>PATCH /api/admin/verifications/{verificationId}</b> — 학생 인증 심사 (승인·반려)
     * <p>
     * - 심사 대상은 review_status = PENDING 행만. NOT_SUBMITTED면 409 CAPTURE_NOT_SUBMITTED, APPROVED·REJECTED면 409 ALREADY_REVIEWED (3.3.0)
     * - APPROVED: 제출 학번이 USERS.student_no와 일치해야 함 → USERS.is_verified = 1
     * - REJECTED: rejectReason 필수, 회원은 재제출 가능
     * - 공통: reviewed_at = now, 결과를 SYSTEM 알림으로 발송
     * <p>
     * <b>에러 코드</b>
     * - 400: REJECT_REASON_REQUIRED / STUDENT_NO_MISMATCH
     * - 403: FORBIDDEN
     * - 409: ALREADY_REVIEWED / CAPTURE_NOT_SUBMITTED
     */
    @Transactional
    public AdminVerificationResponse reviewVerification(Long adminId, Long verificationId, VerificationReviewRequest request) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/admin/verifications/{verificationId}");
    }

    /**
     * <b>GET /api/admin/verifications/{verificationId}/capture</b> — 학생 인증 캡처 이미지 조회 (3.3.0)
     * <p>
     * 서버 로컬 폴더에 저장된 캡처 이미지를 반환 (운영자 전용, 외부 공개 URL 없음). 캡처 미제출이거나 파일이 없으면 404.
     * 운영자 웹은 `fetch` + Authorization 헤더 → `blob()` → `URL.createObjectURL()`로 표시 (`&lt;img src>` 직접 지정 시 401)
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public StoredFile getCaptureImage(Long adminId, Long verificationId) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/verifications/{verificationId}/capture");
    }
}
