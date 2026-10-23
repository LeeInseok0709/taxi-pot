package com.taxipot.user.service;

import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.common.PageResponse;
import com.taxipot.global.file.StoredImage;
import com.taxipot.user.dto.AdminVerificationResponse;
import com.taxipot.user.dto.VerificationReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminVerificationController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVerificationService {

    /**
     * GET /api/admin/verifications — 학생 인증 심사 목록 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * reviewStatus 미지정 시 PENDING(심사 대기)만 반환. NOT_SUBMITTED(캡처 미제출)는 지정할 때만 조회 (3.3.0)
     */
    public PageResponse<AdminVerificationResponse> getVerificationPage(Long adminId, ReviewStatus reviewStatus, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/admin/verifications");
    }

    /**
     * GET /api/admin/verifications/{verificationId} — 학생 인증 상세 조회 (캡처본 열람) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 가입 정보(이름·성별·대학교·학번)와 캡처 이미지 주소(captureImgUrl)를 함께 반환해 대조할 수 있게 한다.
     * 이미지 자체는 captureImgUrl(`/api/admin/verifications/{id}/capture`)을 운영자 토큰으로 요청해서 받는다
     */
    public AdminVerificationResponse getVerification(Long adminId, Long verificationId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/admin/verifications/{verificationId}");
    }

    /**
     * PATCH /api/admin/verifications/{verificationId} — 학생 인증 심사 (승인·반려) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * - 심사 대상은 review_status = PENDING 행만. NOT_SUBMITTED면 409 CAPTURE_NOT_SUBMITTED, APPROVED·REJECTED면 409 ALREADY_REVIEWED (3.3.0)
     * - APPROVED: 제출 학번이 USERS.student_no와 일치해야 함 → USERS.is_verified = 1
     * - REJECTED: rejectReason 필수, 회원은 재제출 가능
     * - 공통: reviewed_at = now, 결과를 SYSTEM 알림으로 발송
     */
    @Transactional
    public AdminVerificationResponse reviewVerification(Long adminId, Long verificationId, VerificationReviewRequest request) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/admin/verifications/{verificationId}");
    }

    /**
     * GET /api/admin/verifications/{verificationId}/capture — 학생 인증 캡처 이미지 조회 (3.3.0) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 서버 로컬 폴더에 저장된 캡처 이미지를 반환 (운영자 전용, 외부 공개 URL 없음). 캡처 미제출이거나 파일이 없으면 404.
     * 운영자 웹은 `fetch` + Authorization 헤더 → `blob()` → `URL.createObjectURL()`로 표시 (`<img src>` 직접 지정 시 401)
     */
    public StoredImage getCaptureImage(Long adminId, Long verificationId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/admin/verifications/{verificationId}/capture");
    }
}
