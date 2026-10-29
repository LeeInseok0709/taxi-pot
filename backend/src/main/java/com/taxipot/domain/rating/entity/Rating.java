package com.taxipot.domain.rating.entity;

import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RATING. avg_score 는 DB 계산 컬럼 → INSERT·UPDATE 에서 제외 (값을 넣으면 MySQL 에러) */
@Getter
@Entity
@Table(name = "RATING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "noshow_score", nullable = false)
    private Integer noshowScore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "payment_score", nullable = false)
    private Integer paymentScore;

    /** 저장 직후 객체에서는 null. 필요하면 averageScore() 사용 */
    @Column(name = "avg_score", precision = 3, scale = 2, insertable = false, updatable = false)
    private BigDecimal avgScore;

    @Column(name = "bad_reason", length = 255)
    private String badReason;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Kst.now();
    }

    public static Rating submit(Long potId, Long reviewerId, Long revieweeId,
                                int noshow, int manner, int communication, int payment, String badReason) {
        Rating r = new Rating();
        r.potId = potId;
        r.reviewerId = reviewerId;
        r.revieweeId = revieweeId;
        r.noshowScore = noshow;
        r.mannerScore = manner;
        r.communicationScore = communication;
        r.paymentScore = payment;
        r.badReason = badReason;
        r.isDefault = false;
        return r;
    }

    /** 스케줄러 7번: 미제출 평가쌍 ★3 기본값 (평점 변동 없음) */
    public static Rating defaultOf(Long potId, Long reviewerId, Long revieweeId) {
        Rating r = submit(potId, reviewerId, revieweeId, 3, 3, 3, 3, null);
        r.isDefault = true;
        return r;
    }

    /** 4항목 평균 (DB avg_score 와 같은 값) */
    public BigDecimal averageScore() {
        return BigDecimal.valueOf(noshowScore + mannerScore + communicationScore + paymentScore)
                .divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
    }
}
