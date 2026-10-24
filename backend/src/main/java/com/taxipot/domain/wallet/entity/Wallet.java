package com.taxipot.domain.wallet.entity;

import com.taxipot.global.error.Errors;
import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WALLET (updated_at 만 있음 → BaseTimeEntity 상속 금지).
 * <p>금액을 직접 바꾸지 말고 WalletLedger 를 거친다 (원장 POINT_HISTORY 기록이 함께 남아야 함).
 */
@Getter
@Entity
@Table(name = "WALLET")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 사용 가능 잔액 */
    @Column(name = "balance", nullable = false)
    private Integer balance;

    /** 예치 금액 (택시팟 참여 중 묶인 돈) */
    @Column(name = "held_balance", nullable = false)
    private Integer heldBalance;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Kst.now();
    }

    public static Wallet open(Long memberId) {
        Wallet w = new Wallet();
        w.memberId = memberId;
        w.balance = 0;
        w.heldBalance = 0;
        return w;
    }

    public int total() {
        return balance + heldBalance;
    }

    // 아래 메서드는 WalletLedger 에서만 호출한다 (package 밖 호출을 막기 어려워 public)

    /** balance → held_balance */
    public void hold(int amount) {
        if (balance < amount) {
            throw Errors.insufficientBalance(amount, balance);
        }
        balance -= amount;
        heldBalance += amount;
    }

    /** held_balance → balance */
    public void release(int amount) {
        if (heldBalance < amount) {
            throw new IllegalStateException("예치 해제 금액이 예치액보다 큽니다: wallet=" + id);
        }
        heldBalance -= amount;
        balance += amount;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void deduct(int amount) {
        if (balance < amount) {
            throw Errors.insufficientBalance(amount, balance);
        }
        balance -= amount;
    }
}
