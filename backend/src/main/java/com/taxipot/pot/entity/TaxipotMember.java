package com.taxipot.pot.entity;

import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PreferredSeat;
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
 * TAXIPOT_MEMBER (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 pot 담당만.
 */
@Getter
@Entity
@Table(name = "TAXIPOT_MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxipotMember {

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

    /** 역할 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 10)
    private PotMemberRole role = PotMemberRole.GUEST;

    /** 희망 좌석 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "preferred_seat", nullable = false, length = 12)
    private PreferredSeat preferredSeat = PreferredSeat.NONE;

    /** 참여 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PotMemberStatus status = PotMemberStatus.JOINED;

    /** 도착 확인 일시 */
    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    /** 참여 신청 일시 */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /** 취소·퇴장 일시 */
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Builder
    private TaxipotMember(Long potId, Long memberId, PotMemberRole role, PreferredSeat preferredSeat, PotMemberStatus status, LocalDateTime joinedAt) {
        this.potId = potId;
        this.memberId = memberId;
        if (role != null) {
            this.role = role;
        }
        if (preferredSeat != null) {
            this.preferredSeat = preferredSeat;
        }
        if (status != null) {
            this.status = status;
        }
        this.joinedAt = joinedAt;
    }

    // ===== 상태 변경 메서드 (pot 담당이 추가) =====
}
