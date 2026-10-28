package com.taxipot.domain.pot.repository;

import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.global.code.PotMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
