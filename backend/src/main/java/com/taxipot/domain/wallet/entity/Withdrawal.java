package com.taxipot.domain.wallet.entity;

import com.taxipot.global.code.TransferProvider;
import com.taxipot.global.code.WithdrawalStatus;
import com.taxipot.global.crypto.EncryptedStringConverter;
import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/** WITHDRAWAL: 등록 계좌로 출금 (시연은 DUMMY 송금 즉시 완료). 계좌 정보는 신청 시점 스냅샷 */
@Getter
@Entity
@Table(name = "WITHDRAWAL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_no", length = 255)
    private String accountNo;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "transfer_provider", nullable = false, length = 20)
    private TransferProvider transferProvider;

    @Column(name = "transfer_tid", length = 100)
    private String transferTid;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private WithdrawalStatus status;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        requestedAt = Kst.now();
    }

    public static Withdrawal request(Long memberId, int amount, String bankCode, String accountDigits, String holder) {
        Withdrawal w = new Withdrawal();
        w.memberId = memberId;
        w.amount = amount;
        w.bankCode = bankCode;
        w.accountNo = accountDigits;
        w.accountHolder = holder;
        w.transferProvider = TransferProvider.DUMMY;
        w.status = WithdrawalStatus.REQUESTED;
        return w;
    }

    public void complete(String tid, LocalDateTime now) {
        this.status = WithdrawalStatus.COMPLETED;
        this.transferTid = tid;
        this.completedAt = now;
    }

    public void fail(String reason, LocalDateTime now) {
        this.status = WithdrawalStatus.FAILED;
        this.failReason = reason;
        this.completedAt = now;
    }

    /** 스케줄러 6번: 탈퇴 회원 계좌 스냅샷 파기 */
    public void purgeAccount() {
        this.bankCode = null;
        this.accountNo = null;
        this.accountHolder = null;
    }
}
