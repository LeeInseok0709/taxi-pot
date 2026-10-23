package com.taxipot.rating.entity;

import com.taxipot.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * RATING (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 rating 담당만.
 */
@Getter
@Entity
@Table(name = "RATING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 택시팟 번호 */
    @Column(name = "pot_id", nullable = false)
    private Long potId;

    /** 평가자 회원번호 */
    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    /** 피평가자 회원번호 */
    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    /** 시간 약속 별점(1~5) */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "noshow_score", nullable = false)
    private Integer noshowScore = 3;

    /** 언행·태도 별점(1~5) */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore = 3;

    /** 연락 별점(1~5) */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore = 3;

    /** 정산 별점(1~5) */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "payment_score", nullable = false)
    private Integer paymentScore = 3;

    /** 4항목 평균 별점 (DB 계산 컬럼 — 읽기 전용) */
    @Column(name = "avg_score", precision = 3, scale = 2, insertable = false, updatable = false)
    private BigDecimal avgScore;

    /** 별로였어요 사유 */
    @Column(name = "bad_reason", length = 255)
    private String badReason;

    /** 자동 기본 평가 여부 */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Builder
    private Rating(Long potId, Long reviewerId, Long revieweeId, Integer noshowScore, Integer mannerScore, Integer communicationScore, Integer paymentScore, String badReason, Boolean isDefault) {
        this.potId = potId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        if (noshowScore != null) {
            this.noshowScore = noshowScore;
        }
        if (mannerScore != null) {
            this.mannerScore = mannerScore;
        }
        if (communicationScore != null) {
            this.communicationScore = communicationScore;
        }
        if (paymentScore != null) {
            this.paymentScore = paymentScore;
        }
        this.badReason = badReason;
        if (isDefault != null) {
            this.isDefault = isDefault;
        }
    }

    // ===== 상태 변경 메서드 (rating 담당이 추가) =====
}
