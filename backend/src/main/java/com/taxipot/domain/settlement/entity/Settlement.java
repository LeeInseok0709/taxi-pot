package com.taxipot.domain.settlement.entity;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;
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

/**
 * SETTLEMENT: 참여자별 예치·정산·미수금. UNIQUE(pot_id, member_id) → 재참여는 rehold() 로 기존 행 UPDATE.
 * <p>DB CHECK chk_settlement_settled_values / chk_settlement_debt_state 를 지키도록 값들을 함께 바꾼다.
 */
@Getter
@Entity
@Table(name = "SETTLEMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "held_amount", nullable = false)
    private Integer heldAmount;

    @Column(name = "fare_share")
    private Integer fareShare;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "diff_amount")
    private Integer diffAmount;

    @Column(name = "uncovered_amount", nullable = false)
    private Integer uncoveredAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "debt_status", nullable = false, length = 10)
    private DebtStatus debtStatus;

    @Column(name = "debt_amount", nullable = false)
    private Integer debtAmount;

    @Column(name = "debt_due_at")
    private LocalDateTime debtDueAt;

    @Column(name = "debt_paid_at")
    private LocalDateTime debtPaidAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private SettlementStatus status;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Kst.now();
    }

    public static Settlement hold(Long potId, Long memberId, int heldAmount) {
        Settlement s = new Settlement();
        s.potId = potId;
        s.memberId = memberId;
        s.resetToHeld(heldAmount);
        return s;
    }

    /** 취소 이력 있는 행으로 재참여 */
    public void rehold(int heldAmount) {
        resetToHeld(heldAmount);
    }

    private void resetToHeld(int heldAmount) {
        this.heldAmount = heldAmount;
        this.status = SettlementStatus.HELD;
        this.fareShare = null;
        this.feeAmount = null;
        this.diffAmount = null;
        this.uncoveredAmount = 0;
        this.debtStatus = DebtStatus.NONE;
        this.debtAmount = 0;
        this.debtDueAt = null;
        this.debtPaidAt = null;
        this.settledAt = null;
    }

    /** 취소·퇴장·노쇼·방 취소: 전액 환불 (분담금 등은 NULL) */
    public void refund() {
        this.status = SettlementStatus.REFUNDED;
        this.fareShare = null;
        this.feeAmount = null;
        this.diffAmount = null;
        this.uncoveredAmount = 0;
        this.debtStatus = DebtStatus.NONE;
        this.debtAmount = 0;
        this.debtDueAt = null;
    }

    /**
     * 정산 완료. uncovered > 0 이면 미수금 UNPAID (debtDueAt = 승인 시각 + 3일).
     * @param diffAmount 정산 차액 = held_amount − (fare_share + fee_amount)
     */
    public void settle(int fareShare, int feeAmount, int diffAmount, int uncovered, LocalDateTime now, LocalDateTime debtDueAt) {
        this.status = SettlementStatus.SETTLED;
        this.fareShare = fareShare;
        this.feeAmount = feeAmount;
        this.diffAmount = diffAmount;
        this.uncoveredAmount = uncovered;
        this.settledAt = now;
        if (uncovered > 0) {
            this.debtStatus = DebtStatus.UNPAID;
            this.debtAmount = uncovered;
            this.debtDueAt = debtDueAt;
        }
    }

    public boolean hasDebt() {
        return debtStatus == DebtStatus.UNPAID || debtStatus == DebtStatus.OVERDUE;
    }

    /** 미수금 납부 (부분 가능). 0 이 되면 PAID */
    public void payDebt(int amount, LocalDateTime now) {
        if (amount <= 0 || amount > debtAmount) {
            throw new IllegalArgumentException("납부액 오류: " + amount);
        }
        this.debtAmount -= amount;
        if (debtAmount == 0) {
            this.debtStatus = DebtStatus.PAID;
            this.debtPaidAt = now;
        }
    }

    /** 스케줄러 8번: 금액은 바꾸지 않음 */
    public void markDebtOverdue() {
        if (debtStatus == DebtStatus.UNPAID) {
            this.debtStatus = DebtStatus.OVERDUE;
        }
    }
}
