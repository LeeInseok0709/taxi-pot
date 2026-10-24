package com.taxipot.wallet.entity;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;
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
 * SETTLEMENT (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 wallet 담당만.
 */
@Getter
@Entity
@Table(name = "SETTLEMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 택시팟 번호 */
    @Column(name = "pot_id", nullable = false)
    private Long potId;

    /** 회원 번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 선결제 예치액 */
    @Column(name = "held_amount", nullable = false)
    private Integer heldAmount;

    /** 최종 택시비 분담금 */
    @Column(name = "fare_share")
    private Integer fareShare;

    /** 서비스 수수료(정산 시점 기록) */
    @Column(name = "fee_amount")
    private Integer feeAmount;

    /** 정산 차액 */
    @Column(name = "diff_amount")
    private Integer diffAmount;

    /** 미수 부족분 */
    @Column(name = "uncovered_amount", nullable = false)
    private Integer uncoveredAmount = 0;

    /** 미수금 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "debt_status", nullable = false, length = 10)
    private DebtStatus debtStatus = DebtStatus.NONE;

    /** 남은 미수 납부액 */
    @Column(name = "debt_amount", nullable = false)
    private Integer debtAmount = 0;

    /** 미수금 납부 기한 */
    @Column(name = "debt_due_at")
    private LocalDateTime debtDueAt;

    /** 미수금 납부 완료 시각 */
    @Column(name = "debt_paid_at")
    private LocalDateTime debtPaidAt;

    /** 정산 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private SettlementStatus status = SettlementStatus.HELD;

    /** 정산 완료 시각 */
    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Builder
    private Settlement(Long potId, Long memberId, Integer heldAmount) {
        this.potId = potId;
        this.memberId = memberId;
        this.heldAmount = heldAmount;
    }

    // ===== 상태 변경 메서드 (wallet 담당이 추가) =====

    /**
     * 재참여 예치: 참여 취소(REFUNDED)했던 방에 다시 들어올 때 같은 행을 다시 HELD로 되돌린다.
     * (pot_id, member_id)가 UNIQUE라 새 행을 만들 수 없기 때문 — 명세 POST /members '취소 이력이 있으면 UPDATE'.
     * 정산 값과 미수금 값은 모두 초기 상태로 비운다 (DB CHECK chk_settlement_settled_values·debt_state 충족).
     */
    public void rehold(int amount) {
        this.status = SettlementStatus.HELD;
        this.heldAmount = amount;
        this.fareShare = null;
        this.feeAmount = null;
        this.diffAmount = null;
        this.settledAt = null;
        this.uncoveredAmount = 0;
        this.debtStatus = DebtStatus.NONE;
        this.debtAmount = 0;
        this.debtDueAt = null;
        this.debtPaidAt = null;
    }

    /**
     * 전액 환불: 정산 없이 예치금을 모두 돌려준 상태로 표시한다.
     * (참여 취소·내보내기·노쇼·방 취소·자동 해산 공통)
     */
    public void refund() {
        this.status = SettlementStatus.REFUNDED;
        this.fareShare = null;
        this.feeAmount = null;
        this.diffAmount = null;
    }
}
