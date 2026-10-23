package com.taxipot.user.entity;

import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.crypto.AesGcmStringConverter;
import com.taxipot.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * USERS (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 user 담당만.
 */
@Getter
@Entity
@Table(name = "USERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 대학교 이메일 */
    @Column(name = "email", length = 100)
    private String email;

    /** 비밀번호 해시 */
    @Column(name = "pw_hash", length = 255)
    private String pwHash;

    /** 이름 */
    @Column(name = "name", length = 50)
    private String name;

    /** 닉네임 */
    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    /** 휴대폰번호(AES-256-GCM 암호문) */
    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "phone", length = 255)
    private String phone;

    /** 성별 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gender", length = 1)
    private Gender gender;

    /** 소속 대학교 */
    @Column(name = "university", nullable = false, length = 50)
    private String university;

    /** 학번 */
    @Column(name = "student_no", length = 20)
    private String studentNo;

    /** 학생 인증 완료 여부 */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    /** 평점 */
    @Column(name = "rating_avg", nullable = false, precision = 4, scale = 2)
    private BigDecimal ratingAvg = new BigDecimal("5.00");

    /** 노쇼 누적 횟수 */
    @Column(name = "no_show_count", nullable = false)
    private Integer noShowCount = 0;

    /** 신고 누적 횟수 */
    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    /** 무신고 연속 이용 횟수 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "clean_streak", nullable = false)
    private Integer cleanStreak = 0;

    /** 권한 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role = UserRole.USER;

    /** 계정 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status = UserStatus.ACTIVE;

    /** 정지 만료일시 */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    /** 탈퇴일시 */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /** 토큰 버전 */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    /** 출금 은행 코드 */
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    /** 출금 계좌번호(AES-256-GCM 암호문) */
    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "account_no", length = 255)
    private String accountNo;

    /** 예금주 */
    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    /** 푸시 토큰 */
    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    /** 필수 약관 동의일시 */
    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    /** 마케팅 수신 동의 */
    @Column(name = "agree_marketing", nullable = false)
    private Boolean agreeMarketing = false;

    @Builder
    private User(String email, String pwHash, String name, String nickname, String phone, Gender gender, String university, String studentNo, String bankCode, String accountNo, String accountHolder, LocalDateTime agreedAt, Boolean agreeMarketing) {
        this.email = email;
        this.pwHash = pwHash;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender;
        this.university = university;
        this.studentNo = studentNo;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountHolder = accountHolder;
        this.agreedAt = agreedAt;
        if (agreeMarketing != null) {
            this.agreeMarketing = agreeMarketing;
        }
    }

    // ===== 상태 변경 메서드 (user 담당이 추가) =====

    /** UserManager.suspend 전용. '더 긴 정지 유지' 비교는 UserManager에서 끝낸 뒤 호출한다. until = null이면 영구 정지 */
    public void applySuspension(LocalDateTime until) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    /** 노쇼 집계 (명세 '노쇼 집계 규칙': NO_SHOW 처리 시점에만 1회) */
    public void increaseNoShowCount() {
        this.noShowCount = this.noShowCount + 1;
    }
}
