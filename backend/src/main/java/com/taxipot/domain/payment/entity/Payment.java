package com.taxipot.domain.payment.entity;

import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.code.PgProvider;
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

/** PAYMENT: PORTONE 충전 (캡스톤 범위 외 — 명세만 유지, 구현 선택) */
@Getter
@Entity
@Table(name = "PAYMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "pg_provider", nullable = false, length = 20)
    private PgProvider pgProvider;

    @Column(name = "pg_tid", length = 100)
    private String pgTid;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Kst.now();
    }

    public static Payment request(Long memberId, String orderId, int amount) {
        Payment p = new Payment();
        p.memberId = memberId;
        p.orderId = orderId;
        p.pgProvider = PgProvider.PORTONE;
        p.amount = amount;
        p.status = PaymentStatus.PENDING;
        return p;
    }

    public void markPaid(String pgTid, LocalDateTime now) {
        this.pgTid = pgTid;
        this.status = PaymentStatus.PAID;
        this.paidAt = now;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}
