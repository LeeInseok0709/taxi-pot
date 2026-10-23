package com.taxipot.user.entity;

import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * STUDENT_VERIFICATION (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 user 담당만.
 */
@Getter
@Entity
@Table(name = "STUDENT_VERIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentVerification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 인증 대상 회원번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 제출 학번 */
    @Column(name = "student_no", nullable = false, length = 20)
    private String studentNo;

    /** 이메일 인증코드 */
    @Column(name = "email_code", nullable = false, length = 10)
    private String emailCode;

    /** 인증코드 만료일시 */
    @Column(name = "email_code_expires_at", nullable = false)
    private LocalDateTime emailCodeExpiresAt;

    /** 인증코드 입력 실패 횟수 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "email_code_attempts", nullable = false)
    private Integer emailCodeAttempts = 0;

    /** 이메일 인증 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "email_status", nullable = false, length = 10)
    private EmailStatus emailStatus = EmailStatus.PENDING;

    /** 학생정보 캡처 파일 경로(서버 로컬) */
    @Column(name = "capture_img_path", length = 255)
    private String captureImgPath;

    /** 캡처 심사 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "review_status", nullable = false, length = 15)
    private ReviewStatus reviewStatus = ReviewStatus.NOT_SUBMITTED;

    /** 반려 사유 */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /** 심사일시 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    private StudentVerification(Long memberId, String studentNo, String emailCode, LocalDateTime emailCodeExpiresAt) {
        this.memberId = memberId;
        this.studentNo = studentNo;
        this.emailCode = emailCode;
        this.emailCodeExpiresAt = emailCodeExpiresAt;
    }

    // ===== 상태 변경 메서드 (user 담당이 추가) =====
}
