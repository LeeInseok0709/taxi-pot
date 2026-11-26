package com.taxipot.domain.user.entity;

import com.taxipot.global.code.Gender;
import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.crypto.EncryptedStringConverter;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * USERS. phone·account_no 는 AES-256-GCM 암호문으로 저장되고 필드에는 평문(숫자만)이 들어 있다.
 * <p>@Setter 없음: 상태 변경은 의도가 드러나는 메서드로만 한다.
 */
@Getter
@Entity
@Table(name = "USERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    public static final BigDecimal INITIAL_RATING = new BigDecimal("5.00");
    private static final BigDecimal MIN_RATING = new BigDecimal("1.00");
    private static final BigDecimal MAX_RATING = new BigDecimal("10.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "pw_hash", length = 255)
    private String pwHash;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone", length = 255)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gender", length = 1)
    private Gender gender;

    /** UNIVERSITY.name FK (다른 도메인이므로 문자열로만 보관) */
    @Column(name = "university", nullable = false, length = 50)
    private String university;

    @Column(name = "student_no", length = 20)
    private String studentNo;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Column(name = "rating_avg", nullable = false, precision = 4, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "no_show_count", nullable = false)
    private Integer noShowCount;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "clean_streak", nullable = false)
    private Integer cleanStreak;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    /** SUSPENDED 일 때만 의미. NULL + SUSPENDED = 영구 정지 */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_no", length = 255)
    private String accountNo;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "agree_marketing", nullable = false)
    private Boolean agreeMarketing;

    public static User signUp(String email, String pwHash, String name, String nickname, String phoneDigits,
                              Gender gender, String university, String studentNo,
                              String bankCode, String accountDigits, String accountHolder,
                              boolean agreeMarketing, LocalDateTime agreedAt) {
        User u = new User();
        u.email = email;
        u.pwHash = pwHash;
        u.name = name;
        u.nickname = nickname;
        u.phone = phoneDigits;
        u.gender = gender;
        u.university = university;
        u.studentNo = studentNo;
        u.isVerified = false;
        u.ratingAvg = INITIAL_RATING;
        u.noShowCount = 0;
        u.reportCount = 0;
        u.cleanStreak = 0;
        u.role = UserRole.USER;
        u.status = UserStatus.ACTIVE;
        u.tokenVersion = 0;
        u.bankCode = bankCode;
        u.accountNo = accountDigits;
        u.accountHolder = accountHolder;
        u.agreeMarketing = agreeMarketing;
        u.agreedAt = agreedAt;
        return u;
    }

    /** 운영자 계정 (AdminAccountInitializer 전용). 가입 API 로는 ADMIN 을 만들 수 없다 */
    public static User createAdmin(String email, String pwHash, String university, LocalDateTime now) {
        User u = signUp(email, pwHash, "운영자", "택시팟운영자", "01000000000", Gender.M, university, "ADMIN-0001",
                null, null, null, false, now);
        u.role = UserRole.ADMIN;
        u.isVerified = true;
        return u;
    }

    // ---------- 조회 도우미 ----------

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isWithdrawn() {
        return status == UserStatus.WITHDRAWN;
    }

    /** 정지 중인지 (기간이 지났지만 스케줄러가 아직 해제하지 않은 경우는 정지 아님) */
    public boolean isSuspendedAt(LocalDateTime now) {
        return status == UserStatus.SUSPENDED && (suspendedUntil == null || suspendedUntil.isAfter(now));
    }

    public boolean hasAccount() {
        return bankCode != null;
    }

    // ---------- 상태 변경 ----------

    public void changeFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePhone(String phoneDigits) {
        this.phone = phoneDigits;
    }

    public void changeAgreeMarketing(boolean agree) {
        this.agreeMarketing = agree;
    }

    /** 비밀번호 변경: token_version + 1 → 기존 토큰 전부 무효 (TOKEN_REVOKED) */
    public void changePassword(String newPwHash) {
        this.pwHash = newPwHash;
        this.tokenVersion++;
    }

    public void registerAccount(String bankCode, String accountDigits, String accountHolder) {
        this.bankCode = bankCode;
        this.accountNo = accountDigits;
        this.accountHolder = accountHolder;
    }

    /** 캡처본 심사 승인 → 학생 인증 완료 */
    public void markVerified() {
        this.isVerified = true;
    }

    /**
     * 정지 (명세 '정지 규칙 통일'): 기존 정지와 비교해 더 긴 쪽을 남긴다.
     * @param until 해제 시각, null = 영구 정지
     */
    public void suspend(LocalDateTime until) {
        if (status == UserStatus.WITHDRAWN) {
            return;
        }
        if (status == UserStatus.SUSPENDED) {
            if (suspendedUntil == null) {
                return; // 이미 영구 정지
            }
            if (until != null && !until.isAfter(suspendedUntil)) {
                return; // 기존 정지가 더 길다
            }
        }
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    /** 정지 해제 (스케줄러 5번: 기간 만료 / 운영자 수동 해제). 영구 정지는 운영자만 호출한다 */
    public void releaseSuspension() {
        if (status == UserStatus.SUSPENDED) {
            this.status = UserStatus.ACTIVE;
            this.suspendedUntil = null;
        }
    }

    public void increaseNoShowCount() {
        this.noShowCount++;
    }

    /** 신고 승인 누적 → 반환값으로 정지 기간 판단(3·6·10회)은 서비스에서 */
    public int increaseReportCount() {
        this.reportCount++;
        this.cleanStreak = 0;
        return reportCount;
    }

    /** 신고 없이 탑승 완료 1회. 3회가 되면 report_count − 1 후 0으로 (정지는 그대로) */
    public void increaseCleanStreak() {
        this.cleanStreak++;
        if (cleanStreak >= 3) {
            if (reportCount > 0) {
                reportCount--;
            }
            cleanStreak = 0;
        }
    }

    /** 평점 변화 (±0.2 등). 1.00 ~ 10.00 범위 유지 */
    public void changeRating(BigDecimal delta) {
        BigDecimal next = ratingAvg.add(delta).setScale(2, RoundingMode.HALF_UP);
        this.ratingAvg = next.max(MIN_RATING).min(MAX_RATING);
    }

    /** 탈퇴 (논리 삭제): 개인정보는 14일 후 스케줄러 6번이 파기 */
    public void withdraw(LocalDateTime now) {
        this.status = UserStatus.WITHDRAWN;
        this.suspendedUntil = null;
        this.withdrawnAt = now;
        this.tokenVersion++;
        this.fcmToken = null;
    }

    /** 스케줄러 6번: 탈퇴 14일 후 개인정보 파기 */
    public void purgePersonalInfo() {
        this.email = null;
        this.pwHash = null;
        this.name = null;
        this.phone = null;
        this.gender = null;
        this.studentNo = null;
        this.fcmToken = null;
        this.bankCode = null;
        this.accountNo = null;
        this.accountHolder = null;
        this.nickname = "탈퇴회원_" + id;
    }
}
