package com.taxipot.domain.verification.service;

import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.verification.dto.CaptureSubmitRequest;
import com.taxipot.domain.verification.dto.EmailCodeConfirmRequest;
import com.taxipot.domain.verification.dto.EmailCodeSendResponse;
import com.taxipot.domain.verification.dto.VerificationResponse;
import com.taxipot.domain.verification.entity.StudentVerification;
import com.taxipot.domain.verification.mail.EmailCodeSender;
import com.taxipot.domain.verification.repository.StudentVerificationRepository;
import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 담당: 공통·인증 (구현 완료) — 이메일 인증코드 발송·재발송·확인, 캡처본 제출, 내 인증 상태.
 * 운영자 심사(승인·반려)는 AdminVerificationService (회원·운영 담당).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailCodeSender emailCodeSender;
    private final FileStorage fileStorage;
    private final AppProperties props;
    private final TimeProvider timeProvider;
    private final SecureRandom random = new SecureRandom();

    /** 회원가입 트랜잭션 안에서 호출: 인증 행 생성 + (커밋 후) 코드 메일 발송 */
    @Transactional
    public StudentVerification issueForSignup(User user) {
        String code = newCode();
        StudentVerification v = verificationRepository.save(
                StudentVerification.forSignup(user.getId(), user.getStudentNo(), code, expiresAt(timeProvider.now())));
        sendAfterCommit(user.getEmail(), code);
        return v;
    }

    /** POST /api/verifications/email-code — 재발송 (직전 발송 후 60초 이내면 429, 실패 횟수 0 초기화) */
    @Transactional
    public EmailCodeSendResponse resendEmailCode(Long userId) {
        if (verificationRepository.existsByMemberIdAndEmailStatus(userId, EmailStatus.VERIFIED)) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
        }
        StudentVerification v = verificationRepository.findFirstByMemberIdAndEmailStatusOrderByIdDesc(userId, EmailStatus.PENDING)
                .orElseThrow(() -> Errors.notFound("인증 요청"));
        LocalDateTime now = timeProvider.now();
        LocalDateTime lastSentAt = v.getEmailCodeExpiresAt().minusMinutes(props.verification().emailCodeTtlMinutes());
        if (now.isBefore(lastSentAt.plusSeconds(props.verification().resendIntervalSeconds()))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "인증코드는 60초 후에 다시 받을 수 있습니다.");
        }
        String code = newCode();
        v.reissueCode(code, expiresAt(now));
        User user = userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원"));
        sendAfterCommit(user.getEmail(), code);
        return new EmailCodeSendResponse(v.getId(), user.getEmail(), Kst.toOffset(v.getEmailCodeExpiresAt()));
    }

    /**
     * POST /api/verifications/email-code/confirm.
     * 검증 순서: 실패 횟수(5회 이상 409) → 만료(400) → 코드 일치(틀리면 횟수 + 1 후 400, detail.remainingAttempts).
     * 실패 횟수 증가는 예외를 던져도 커밋돼야 하므로 noRollbackFor.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public VerificationResponse confirmEmailCode(Long userId, EmailCodeConfirmRequest request) {
        if (verificationRepository.existsByMemberIdAndEmailStatus(userId, EmailStatus.VERIFIED)) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
        }
        StudentVerification v = verificationRepository.findFirstByMemberIdAndEmailStatusOrderByIdDesc(userId, EmailStatus.PENDING)
                .orElseThrow(() -> Errors.notFound("인증 요청"));
        int max = props.verification().emailCodeMaxAttempts();
        if (v.getEmailCodeAttempts() >= max) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
        }
        if (timeProvider.now().isAfter(v.getEmailCodeExpiresAt())) {
            throw new BusinessException(ErrorCode.EXPIRED_EMAIL_CODE);
        }
        if (!MessageDigest.isEqual(v.getEmailCode().getBytes(StandardCharsets.UTF_8),
                request.emailCode().trim().getBytes(StandardCharsets.UTF_8))) {
            int updated = verificationRepository.increaseAttempts(v.getId(), max);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
            }
            throw Errors.invalidEmailCode(max - (v.getEmailCodeAttempts() + 1));
        }
        v.verifyEmail();
        return toResponse(v);
    }

    /**
     * POST /api/verifications/capture (multipart). 이메일 인증 후에만, 학번 일치, 최근 행 상태에 따라
     * NOT_SUBMITTED → 그 행에 저장 / PENDING → 409 REVIEW_IN_PROGRESS / APPROVED → 409 ALREADY_VERIFIED / REJECTED → 새 행.
     */
    @Transactional
    public VerificationResponse submitCapture(Long userId, CaptureSubmitRequest request) {
        if (!verificationRepository.existsByMemberIdAndEmailStatus(userId, EmailStatus.VERIFIED)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> Errors.notFound("회원"));
        if (!request.studentNo().trim().equals(user.getStudentNo())) {
            throw new BusinessException(ErrorCode.STUDENT_NO_MISMATCH);
        }
        StudentVerification latest = verificationRepository.findFirstByMemberIdOrderByIdDesc(userId)
                .orElseThrow(() -> Errors.notFound("인증 요청"));
        switch (latest.getReviewStatus()) {
            case PENDING -> throw new BusinessException(ErrorCode.REVIEW_IN_PROGRESS);
            case APPROVED -> throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
            default -> {
                // NOT_SUBMITTED 또는 REJECTED → 아래에서 저장
            }
        }
        String path = fileStorage.saveImage(request.captureImage(), FileStorage.DIR_CAPTURE + "/" + userId);
        StudentVerification target;
        if (latest.getReviewStatus() == com.taxipot.global.code.ReviewStatus.NOT_SUBMITTED) {
            latest.submitCapture(path);
            target = latest;
        } else {
            target = verificationRepository.save(StudentVerification.forResubmit(latest, request.studentNo().trim(), path));
        }
        return toResponse(target);
    }

    /** GET /api/verifications/me — 가장 최근 행 기준 */
    @Transactional(readOnly = true)
    public VerificationResponse getMyVerification(Long userId) {
        return verificationRepository.findFirstByMemberIdOrderByIdDesc(userId)
                .map(VerificationService::toResponse)
                .orElseThrow(() -> Errors.notFound("인증 정보"));
    }

    public static VerificationResponse toResponse(StudentVerification v) {
        return new VerificationResponse(v.getId(), v.getStudentNo(), v.getEmailStatus(), v.getReviewStatus(),
                v.getRejectReason(), Kst.toOffset(v.getReviewedAt()), Kst.toOffset(v.getCreatedAt()));
    }

    private LocalDateTime expiresAt(LocalDateTime now) {
        return now.plusMinutes(props.verification().emailCodeTtlMinutes());
    }

    private String newCode() {
        int length = props.verification().emailCodeLength();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void sendAfterCommit(String email, String code) {
        long ttl = props.verification().emailCodeTtlMinutes();
        Runnable send = () -> {
            try {
                emailCodeSender.send(email, code, ttl);
            } catch (RuntimeException e) {
                log.error("인증코드 메일 발송 실패: {}", email, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
