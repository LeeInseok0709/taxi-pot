package com.taxipot.domain.verification.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.verification.dto.AdminVerificationPageResponse;
import com.taxipot.domain.verification.dto.AdminVerificationResponse;
import com.taxipot.domain.verification.dto.VerificationReviewRequest;
import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.domain.verification.repository.StudentVerificationRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.file.StoredFile;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final NotificationSender notificationSender;
    private final TimeProvider timeProvider;

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
        // 미지정이면 심사 대기(PENDING)만. 먼저 제출한 순서대로 심사하도록 오래된 순 정렬
        ReviewStatus status = reviewStatus == null ? ReviewStatus.PENDING : reviewStatus;
        Page<StudentVerification> result = verificationRepository.findAllByReviewStatus(status,
                Paging.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt", "id")));

        // 회원 정보는 한 번에 조회 (행마다 조회하면 N+1)
        Map<Long, User> users = userRepository.findAllById(
                        result.getContent().stream().map(StudentVerification::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return new AdminVerificationPageResponse(
                result.getContent().stream().map(v -> toResponse(v, users.get(v.getMemberId()))).toList(),
                PageMeta.from(result));
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
        StudentVerification v = findVerification(verificationId);
        return toResponse(v, userRepository.findById(v.getMemberId()).orElse(null));
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
        // 1. 허용 값은 APPROVED·REJECTED 만
        ReviewStatus decision = request.reviewStatus();
        if (decision != ReviewStatus.APPROVED && decision != ReviewStatus.REJECTED) {
            throw Errors.invalidInput("reviewStatus: APPROVED 또는 REJECTED 만 가능합니다.");
        }

        // 2. 심사 대상은 PENDING 행만. 인증 행을 잠근 뒤 상태를 확인해 두 운영자의 동시 심사를 막는다
        StudentVerification v = verificationRepository.findByIdForUpdate(verificationId)
                .orElseThrow(() -> Errors.notFound("인증 요청"));
        User user = userRepository.findById(v.getMemberId()).orElseThrow(() -> Errors.notFound("회원"));
        switch (v.getReviewStatus()) {
            case NOT_SUBMITTED -> throw new BusinessException(ErrorCode.CAPTURE_NOT_SUBMITTED);
            case APPROVED, REJECTED -> throw new BusinessException(ErrorCode.ALREADY_REVIEWED);
            default -> {
                // PENDING → 심사 진행
            }
        }

        // 3. 승인·반려
        if (decision == ReviewStatus.APPROVED) {
            // 제출 학번이 가입 학번과 같아야 승인 (400 STUDENT_NO_MISMATCH)
            if (v.getStudentNo() == null || !v.getStudentNo().equals(user.getStudentNo())) {
                throw new BusinessException(ErrorCode.STUDENT_NO_MISMATCH);
            }
            v.approve(timeProvider.now());
            user.markVerified();
            notificationSender.send(user.getId(), null, NotificationType.SYSTEM,
                    "학생 인증 완료", "학생 인증이 승인되었습니다. 이제 택시팟을 이용할 수 있습니다.");
        } else {
            // 반려 사유 필수 (400 REJECT_REASON_REQUIRED). 회원은 캡처본을 다시 제출할 수 있다
            String reason = request.rejectReason() == null ? "" : request.rejectReason().trim();
            if (reason.isEmpty()) {
                throw new BusinessException(ErrorCode.REJECT_REASON_REQUIRED);
            }
            v.reject(reason, timeProvider.now());
            notificationSender.send(user.getId(), null, NotificationType.SYSTEM,
                    "학생 인증 반려", "학생 인증이 반려되었습니다. 사유: " + reason + " (캡처본을 다시 제출해 주세요)");
        }
        return toResponse(v, user);
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
        StudentVerification v = findVerification(verificationId);
        if (v.getCaptureImgPath() == null) {
            throw Errors.notFound("캡처 이미지");
        }
        // 파일이 없으면 FileStorage.load 가 404 를 던진다
        return fileStorage.load(v.getCaptureImgPath());
    }

    // ---------- 내부 도우미 ----------

    private StudentVerification findVerification(Long verificationId) {
        return verificationRepository.findById(verificationId).orElseThrow(() -> Errors.notFound("인증 요청"));
    }

    /** 가입 정보(USERS)와 제출 정보(STUDENT_VERIFICATION)를 나란히 보여 주는 응답 */
    private AdminVerificationResponse toResponse(StudentVerification v, User user) {
        String captureUrl = v.getCaptureImgPath() == null
                ? null : "/api/admin/verifications/" + v.getId() + "/capture";
        return new AdminVerificationResponse(
                v.getId(),
                v.getMemberId(),
                user == null ? null : user.getName(),
                user == null ? null : user.getGender(),
                user == null ? null : user.getUniversity(),
                user == null ? null : user.getStudentNo(),
                v.getStudentNo(),
                user == null ? null : user.getEmail(),
                v.getEmailStatus(),
                captureUrl,
                v.getReviewStatus(),
                v.getRejectReason(),
                Kst.toOffset(v.getReviewedAt()),
                Kst.toOffset(v.getCreatedAt()));
    }
}
