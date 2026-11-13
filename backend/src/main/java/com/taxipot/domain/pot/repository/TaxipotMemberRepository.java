package com.taxipot.domain.pot.repository;

import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaxipotMemberRepository extends JpaRepository<TaxipotMember, Long> {

    Optional<TaxipotMember> findByPotIdAndMemberId(Long potId, Long memberId);

    List<TaxipotMember> findAllByPotIdAndStatusInOrderByIdAsc(Long potId, Collection<PotMemberStatus> statuses);

    boolean existsByPotIdAndMemberIdAndStatusIn(Long potId, Long memberId, Collection<PotMemberStatus> statuses);

    /**
     * '진행 중인 다른 방' (명세 핵심 정책): 내 상태 JOINED·BOARDED·REQUESTED 이고
     * 방이 RECRUITING·CLOSED 이거나, RUNNING 이면서 실요금 청구 전(NONE)인 방. 없으면 빈 리스트.
     */
    @Query("select m.potId from TaxipotMember m, Taxipot p "
            + "where p.id = m.potId and m.memberId = :memberId "
            + "and m.status in (com.taxipot.global.code.PotMemberStatus.JOINED, "
            + "                 com.taxipot.global.code.PotMemberStatus.BOARDED, "
            + "                 com.taxipot.global.code.PotMemberStatus.REQUESTED) "
            + "and (p.status in (com.taxipot.global.code.PotStatus.RECRUITING, com.taxipot.global.code.PotStatus.CLOSED) "
            + "     or (p.status = com.taxipot.global.code.PotStatus.RUNNING "
            + "         and p.fareClaimStatus = com.taxipot.global.code.FareClaimStatus.NONE)) "
            + "order by m.id desc")
    List<Long> findActivePotIds(@Param("memberId") Long memberId);

    /**
     * 방의 특정 상태 참여자 member_id 목록 (오름차순, 락 없음).
     * <p>스케줄러가 '누구를 잠글지' 정할 때 쓴다 → 이 목록으로 MemberWalletLocker.lockAll 을 먼저 부르고, 그 뒤 TAXIPOT 을 잠근다.
     */
    @Query("select m.memberId from TaxipotMember m where m.potId = :potId and m.status in :statuses order by m.memberId asc")
    List<Long> findMemberIds(@Param("potId") Long potId, @Param("statuses") Collection<PotMemberStatus> statuses);

    /**
     * 스케줄러 4 (지각 경고) 대상 참여자.
     * <ul>
     *   <li>방 RECRUITING·CLOSED, now &lt; 집결 기한 &lt;= now + N분 (= '집결 기한 N분 전' 구간에 들어온 방)</li>
     *   <li>참여자 JOINED 이고 아직 도착 확인 안 함 (방장 포함)</li>
     *   <li>같은 방·같은 회원에게 LATE_WARNING 알림을 보낸 적 없음 → 중복 발송 방지</li>
     * </ul>
     * 중복 방지를 별도 컬럼 대신 NOTIFICATION 존재 여부로 한다. 이유: 스케줄러가 fixedDelay 라 실행 간격이 정확히 60초가 아니어서
     * '이번 1분 구간만' 같은 시간창 방식은 빠뜨리거나 두 번 보낼 수 있고, DB 컬럼 추가는 설계서 변경이 필요하기 때문.
     */
    @Query("select m from TaxipotMember m, Taxipot p "
            + "where p.id = m.potId "
            + "and p.status in (com.taxipot.global.code.PotStatus.RECRUITING, com.taxipot.global.code.PotStatus.CLOSED) "
            + "and p.arrivalDeadline > :now and p.arrivalDeadline <= :warnUntil "
            + "and m.status = com.taxipot.global.code.PotMemberStatus.JOINED and m.arrivedAt is null "
            + "and not exists (select n.id from Notification n "
            + "                where n.memberId = m.memberId and n.potId = m.potId "
            + "                and n.type = com.taxipot.global.code.NotificationType.LATE_WARNING) "
            + "order by m.potId asc, m.id asc")
    List<TaxipotMember> findLateWarningTargets(@Param("now") LocalDateTime now,
                                               @Param("warnUntil") LocalDateTime warnUntil);

    /** 방의 특정 상태 참여 행 전체 (입장 요청 목록·탑승 시작) */
    List<TaxipotMember> findAllByPotIdAndStatus(Long potId, PotMemberStatus status);

    /**
     * 택시 이용 기록 (GET /api/users/me/pots, GET /api/admin/users/{userId}/pots).
     * SETTLEMENT 가 있는 행만 (입장 요청만 하고 참여하지 못한 REQUESTED·REJECTED·철회 행 제외, 명세 3.3.0). 최신 참여순.
     * role·potStatus 가 null 이면 조건 없음.
     */
    @Query(value = "select m from TaxipotMember m, Taxipot p, Settlement s "
            + "where p.id = m.potId and s.potId = m.potId and s.memberId = m.memberId "
            + "and m.memberId = :memberId "
            + "and m.status not in (com.taxipot.global.code.PotMemberStatus.REQUESTED, "
            + "                     com.taxipot.global.code.PotMemberStatus.REJECTED) "
            + "and (:role is null or m.role = :role) "
            + "and (:potStatus is null or p.status = :potStatus) "
            + "order by m.joinedAt desc, m.id desc",
            countQuery = "select count(m) from TaxipotMember m, Taxipot p, Settlement s "
            + "where p.id = m.potId and s.potId = m.potId and s.memberId = m.memberId "
            + "and m.memberId = :memberId "
            + "and m.status not in (com.taxipot.global.code.PotMemberStatus.REQUESTED, "
            + "                     com.taxipot.global.code.PotMemberStatus.REJECTED) "
            + "and (:role is null or m.role = :role) "
            + "and (:potStatus is null or p.status = :potStatus)")
    Page<TaxipotMember> findHistory(@Param("memberId") Long memberId, @Param("role") PotMemberRole role,
                                    @Param("potStatus") PotStatus potStatus, Pageable pageable);
}
