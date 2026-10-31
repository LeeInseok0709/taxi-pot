package com.taxipot.domain.pot.repository;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.global.code.PotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaxipotRepository extends JpaRepository<Taxipot, Long>, JpaSpecificationExecutor<Taxipot> {

    /** 락 순서 3단계: TAXIPOT (USERS → WALLET 을 먼저 잠근 뒤) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Taxipot p where p.id = :id")
    Optional<Taxipot> findByIdForUpdate(@Param("id") Long id);

    // ================================================================ 스케줄러 '대상 방 id' 조회
    // 모두 락 없이 id 만 읽는다 (트랜잭션 밖). 실제 처리는 PotJobProcessor 가 방마다 락을 잡고 조건을 '다시' 확인한다.
    // → 여기서 읽은 뒤 처리 직전까지 방 상태가 바뀌어도(방장이 탑승 시작 등) 이중 처리가 생기지 않는다.

    /**
     * 스케줄러 1 (미도착 자동 퇴장) 대상.
     * 집결 기한이 지난 RECRUITING·CLOSED 방 중에서 '도착 안 한 JOINED 참여자가 한 명이라도 있는 방'만.
     * <p>EXISTS 조건이 없으면 처리할 사람이 없는 방(전원 도착)도 출발 30분 후 자동 취소될 때까지 매분 다시 잡혀서 쓸데없이 락을 건다.
     */
    @Query("select p.id from Taxipot p "
            + "where p.status in :statuses and p.arrivalDeadline <= :now "
            + "and exists (select m.id from TaxipotMember m "
            + "            where m.potId = p.id "
            + "            and m.status = com.taxipot.global.code.PotMemberStatus.JOINED "
            + "            and m.arrivedAt is null) "
            + "and p.currentMembers >= 2 "   // 방장 단독 방은 스케줄러 2 담당 (PotJobProcessor.processNoShow 주석 참고)
            + "order by p.id asc")
    List<Long> findNoShowTargetIds(@Param("statuses") Collection<PotStatus> statuses,
                                   @Param("now") LocalDateTime now);

    /** 스케줄러 2 (방장 단독 방 자동 해산) 대상: 출발 시각이 지났는데 RECRUITING 이고 방장 혼자 남은 방 */
    @Query("select p.id from Taxipot p "
            + "where p.status = com.taxipot.global.code.PotStatus.RECRUITING "
            + "and p.currentMembers = 1 and p.departureTime <= :now "
            + "order by p.id asc")
    List<Long> findSoloPotIds(@Param("now") LocalDateTime now);

    /**
     * 스케줄러 3 (미출발 방 자동 취소) 대상: departure_time + 30분 &lt;= now 인 RECRUITING·CLOSED 방.
     * <p>'departure_time + 30분 &lt;= now' 를 'departure_time &lt;= now − 30분' 으로 바꿔서 넘긴다
     * (컬럼에 계산을 걸지 않아야 departure_time 인덱스를 탈 수 있다).
     */
    @Query("select p.id from Taxipot p "
            + "where p.status in :statuses and p.departureTime <= :threshold "
            + "order by p.id asc")
    List<Long> findNotStartedPotIds(@Param("statuses") Collection<PotStatus> statuses,
                                    @Param("threshold") LocalDateTime departureThreshold);

    /**
     * 스케줄러 11 (출발 확인 알림) 대상. 명세 조건을 그대로 옮김:
     * <ul>
     *   <li>status RECRUITING·CLOSED, start_reminded_at IS NULL (1회만)</li>
     *   <li>남은 JOINED 중 미도착자가 없음 (전원 arrived_at NOT NULL)</li>
     *   <li>남은 JOINED 가 2명 이상</li>
     *   <li>MAX(arrived_at) + 3분 &lt;= now  →  MAX(arrived_at) &lt;= now − 3분 (:threshold)</li>
     * </ul>
     */
    @Query("select p.id from Taxipot p "
            + "where p.status in :statuses and p.startRemindedAt is null "
            + "and not exists (select m.id from TaxipotMember m "
            + "                where m.potId = p.id "
            + "                and m.status = com.taxipot.global.code.PotMemberStatus.JOINED "
            + "                and m.arrivedAt is null) "
            + "and (select count(m2) from TaxipotMember m2 "
            + "     where m2.potId = p.id and m2.status = com.taxipot.global.code.PotMemberStatus.JOINED) >= 2 "
            + "and (select max(m3.arrivedAt) from TaxipotMember m3 "
            + "     where m3.potId = p.id and m3.status = com.taxipot.global.code.PotMemberStatus.JOINED) <= :threshold "
            + "order by p.id asc")
    List<Long> findStartReminderTargetIds(@Param("statuses") Collection<PotStatus> statuses,
                                          @Param("threshold") LocalDateTime lastArrivalThreshold);
}
