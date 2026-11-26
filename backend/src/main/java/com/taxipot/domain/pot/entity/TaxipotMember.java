package com.taxipot.domain.pot.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * TAXIPOT_MEMBER. UNIQUE(pot_id, member_id) → 같은 방 재참여·재요청은 INSERT 가 아니라 기존 행 UPDATE (rejoin/request).
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

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 10)
    private PotMemberRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "preferred_seat", nullable = false, length = 12)
    private PreferredSeat preferredSeat;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PotMemberStatus status;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private static TaxipotMember of(Long potId, Long memberId, PotMemberRole role, PreferredSeat seat,
                                    PotMemberStatus status, LocalDateTime now) {
        TaxipotMember m = new TaxipotMember();
        m.potId = potId;
        m.memberId = memberId;
        m.role = role;
        m.preferredSeat = seat == null ? PreferredSeat.NONE : seat;
        m.status = status;
        m.joinedAt = now;
        return m;
    }

    public static TaxipotMember host(Long potId, Long memberId, PreferredSeat seat, LocalDateTime now) {
        return of(potId, memberId, PotMemberRole.HOST, seat, PotMemberStatus.JOINED, now);
    }

    public static TaxipotMember guest(Long potId, Long memberId, PreferredSeat seat, LocalDateTime now) {
        return of(potId, memberId, PotMemberRole.GUEST, seat, PotMemberStatus.JOINED, now);
    }

    /** 입장 요청 (참여 마감 후) */
    public static TaxipotMember request(Long potId, Long memberId, PreferredSeat seat, LocalDateTime now) {
        return of(potId, memberId, PotMemberRole.GUEST, seat, PotMemberStatus.REQUESTED, now);
    }

    public boolean isHost() {
        return role == PotMemberRole.HOST;
    }

    public boolean isActive() {
        return status == PotMemberStatus.JOINED || status == PotMemberStatus.BOARDED;
    }

    public boolean hasArrived() {
        return arrivedAt != null;
    }

    /** 취소 이력이 있는 행으로 다시 참여 */
    public void rejoin(PreferredSeat seat, LocalDateTime now) {
        this.status = PotMemberStatus.JOINED;
        this.preferredSeat = seat == null ? PreferredSeat.NONE : seat;
        this.joinedAt = now;
        this.arrivedAt = null;
        this.canceledAt = null;
    }

    /** 취소 이력이 있는 행으로 다시 입장 요청 */
    public void rerequest(PreferredSeat seat, LocalDateTime now) {
        this.status = PotMemberStatus.REQUESTED;
        this.preferredSeat = seat == null ? PreferredSeat.NONE : seat;
        this.joinedAt = now;
        this.arrivedAt = null;
        this.canceledAt = null;
    }

    /** 참여 취소·입장 요청 철회·자동 취소 */
    public void cancel(LocalDateTime now) {
        this.status = PotMemberStatus.CANCELED;
        this.canceledAt = now;
    }

    /** 집결 기한 전 방장 내보내기 (노쇼 아님) */
    public void kick(LocalDateTime now) {
        this.status = PotMemberStatus.KICKED;
        this.canceledAt = now;
    }

    /** 집결 기한까지 미도착 퇴장 (스케줄러 1번 또는 기한 후 방장 내보내기) */
    public void markNoShow(LocalDateTime now) {
        this.status = PotMemberStatus.NO_SHOW;
        this.canceledAt = now;
    }

    public void arrive(LocalDateTime now) {
        this.arrivedAt = now;
    }

    /** 입장 요청 수락: arrived_at = 수락 시각 (들어오자마자 노쇼 처리되지 않게) */
    public void acceptRequest(LocalDateTime now) {
        this.status = PotMemberStatus.JOINED;
        this.arrivedAt = now;
    }

    public void rejectRequest(LocalDateTime now) {
        this.status = PotMemberStatus.REJECTED;
        this.canceledAt = now;
    }

    /** 탑승 시작 시 JOINED → BOARDED */
    public void board() {
        this.status = PotMemberStatus.BOARDED;
    }

    public void changeSeat(PreferredSeat seat) {
        this.preferredSeat = seat;
    }
}
