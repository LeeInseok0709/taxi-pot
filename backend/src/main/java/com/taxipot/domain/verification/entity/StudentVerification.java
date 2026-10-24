package com.taxipot.domain.verification.entity;

import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** STUDENT_VERIFICATION: 이메일 인증(1차) + 캡처본 심사(2차). 재제출 시 새 행 (명세 /api/verifications/capture) */
@Getter
@Entity
@Table(name = "STUDENT_VERIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "student_no", nullable = false, length = 20)
    private String studentNo;

    @Column(name = "email_code", nullable = false, length = 10)
    private String emailCode;

    @Column(name = "email_code_expires_at", nullable = false)
    private LocalDateTime emailCodeExpiresAt;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "email_code_attempts", nullable = false)
    private Integer emailCodeAttempts;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "email_status", nullable = false, length = 10)
    private EmailStatus emailStatus;

    @Column(name = "capture_img_path", length = 255)
    private String captureImgPath;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "review_status", nullable = false, length = 15)
    private ReviewStatus reviewStatus;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Kst.now();
        }
    }

    /** 가입 시 최초 행 (이메일 인증 대기) */
    public static StudentVerification forSignup(Long memberId, String studentNo, String code, LocalDateTime expiresAt) {
        StudentVerification v = new StudentVerification();
        v.memberId = memberId;
        v.studentNo = studentNo;
        v.emailCode = code;
        v.emailCodeExpiresAt = expiresAt;
        v.emailCodeAttempts = 0;
        v.emailStatus = EmailStatus.PENDING;
        v.reviewStatus = ReviewStatus.NOT_SUBMITTED;
        return v;
    }

    /** 반려 후 캡처 재제출: 이메일 인증 상태를 VERIFIED 로 복사한 새 행 (email_code 는 검증 대상 아님) */
    public static StudentVerification forResubmit(StudentVerification previous, String studentNo, String capturePath) {
        StudentVerification v = new StudentVerification();
        v.memberId = previous.memberId;
        v.studentNo = studentNo;
        v.emailCode = previous.emailCode;
        v.emailCodeExpiresAt = previous.emailCodeExpiresAt;
        v.emailCodeAttempts = previous.emailCodeAttempts;
        v.emailStatus = EmailStatus.VERIFIED;
        v.captureImgPath = capturePath;
        v.reviewStatus = ReviewStatus.PENDING;
        return v;
    }

    public void reissueCode(String code, LocalDateTime expiresAt) {
        this.emailCode = code;
        this.emailCodeExpiresAt = expiresAt;
        this.emailCodeAttempts = 0;
    }

    public void verifyEmail() {
        this.emailStatus = EmailStatus.VERIFIED;
    }

    public void submitCapture(String capturePath) {
        this.captureImgPath = capturePath;
        this.reviewStatus = ReviewStatus.PENDING;
    }

    public void approve(LocalDateTime now) {
        this.reviewStatus = ReviewStatus.APPROVED;
        this.rejectReason = null;
        this.reviewedAt = now;
    }

    public void reject(String reason, LocalDateTime now) {
        this.reviewStatus = ReviewStatus.REJECTED;
        this.rejectReason = reason;
        this.reviewedAt = now;
    }
}
