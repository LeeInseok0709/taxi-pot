package com.taxipot.wallet.entity;

import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.code.PgProvider;
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
 * PAYMENT (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 wallet 담당만.
 */
@Getter
@Entity
@Table(name = "PAYMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 결제 회원번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 주문번호 */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /** PG사 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "pg_provider", nullable = false, length = 20)
    private PgProvider pgProvider = PgProvider.PORTONE;

    /** PG 거래번호 */
    @Column(name = "pg_tid", length = 100)
    private String pgTid;

    /** 충전 결제 금액 */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /** 결제 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** 결제 승인 시각 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Payment(Long memberId, String orderId, PgProvider pgProvider, Integer amount) {
        this.memberId = memberId;
        this.orderId = orderId;
        if (pgProvider != null) {
            this.pgProvider = pgProvider;
        }
        this.amount = amount;
    }

    // ===== 상태 변경 메서드 (wallet 담당이 추가) =====
}
