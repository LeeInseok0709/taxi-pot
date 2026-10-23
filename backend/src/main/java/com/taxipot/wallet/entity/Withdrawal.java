package com.taxipot.wallet.entity;

import com.taxipot.global.code.TransferProvider;
import com.taxipot.global.code.WithdrawalStatus;
import com.taxipot.global.crypto.AesGcmStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * WITHDRAWAL (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 wallet 담당만.
 */
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "WITHDRAWAL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 출금 회원번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 출금 금액 */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /** 은행 코드(신청 시점) */
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    /** 계좌번호(AES-256-GCM 암호문, 신청 시점) */
    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "account_no", length = 255)
    private String accountNo;

    /** 예금주(신청 시점) */
    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    /** 송금 처리 방식 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "transfer_provider", nullable = false, length = 20)
    private TransferProvider transferProvider = TransferProvider.DUMMY;

    /** 송금 거래번호 */
    @Column(name = "transfer_tid", length = 100)
    private String transferTid;

    /** 출금 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private WithdrawalStatus status = WithdrawalStatus.REQUESTED;

    /** 실패 사유 */
    @Column(name = "fail_reason", length = 255)
    private String failReason;

    /** 출금 신청 일시 */
    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /** 처리 완료 일시 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private Withdrawal(Long memberId, Integer amount, String bankCode, String accountNo, String accountHolder) {
        this.memberId = memberId;
        this.amount = amount;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountHolder = accountHolder;
    }

    // ===== 상태 변경 메서드 (wallet 담당이 추가) =====
}
