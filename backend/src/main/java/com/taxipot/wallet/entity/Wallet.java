package com.taxipot.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * WALLET (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 wallet 담당만.
 */
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "WALLET")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 소유 회원번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 사용 가능 잔액 */
    @Column(name = "balance", nullable = false)
    private Integer balance = 0;

    /** 예치 금액 */
    @Column(name = "held_balance", nullable = false)
    private Integer heldBalance = 0;

    /** 수정일시 */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Wallet(Long memberId) {
        this.memberId = memberId;
    }

    // ===== 상태 변경 메서드 (wallet 담당이 추가) =====

    /**
     * 예치: 사용 가능 잔액(balance)에서 amount만큼 예치금(held_balance)으로 옮긴다.
     * 잔액 부족 검사는 WalletManager.hold에서 먼저 한다 (여기서는 옮기기만).
     */
    public void hold(int amount) {
        this.balance -= amount;
        this.heldBalance += amount;
    }

    /**
     * 예치 해제: 예치금(held_balance)에서 amount만큼 사용 가능 잔액(balance)으로 되돌린다.
     * 환불(refundHold)과 정산의 FARE_REFUND 단계에서 쓴다.
     */
    public void releaseHold(int amount) {
        this.heldBalance -= amount;
        this.balance += amount;
    }
}
