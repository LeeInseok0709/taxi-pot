package com.taxipot.domain.wallet.entity;

import com.taxipot.global.code.PointType;
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

/** POINT_HISTORY 원장. amount = balance 증감, balance_after = 거래 직후 balance. 수정·삭제 없음 */
@Getter
@Entity
@Table(name = "POINT_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "pot_id")
    private Long potId;

    /** CHARGE·CHARGE_REFUND 만 필수 (DB CHECK) */
    @Column(name = "payment_id")
    private Long paymentId;

    /** WITHDRAW·WITHDRAW_REFUND 만 필수 (DB CHECK) */
    @Column(name = "withdrawal_id")
    private Long withdrawalId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 15)
    private PointType type;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Kst.now();
    }

    public static PointHistory of(Long memberId, PointType type, int amount, int balanceAfter,
                                  Long potId, Long paymentId, Long withdrawalId) {
        PointHistory h = new PointHistory();
        h.memberId = memberId;
        h.type = type;
        h.amount = amount;
        h.balanceAfter = balanceAfter;
        h.potId = potId;
        h.paymentId = paymentId;
        h.withdrawalId = withdrawalId;
        return h;
    }
}
