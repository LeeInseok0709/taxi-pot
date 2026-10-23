package com.taxipot.pot.repository;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.pot.entity.TaxipotMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 조회 메서드 추가는 pot 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface TaxipotMemberRepository extends JpaRepository<TaxipotMember, Long> {

    Optional<TaxipotMember> findByPotIdAndMemberId(Long potId, Long memberId);

    List<TaxipotMember> findByPotIdAndStatusIn(Long potId, Collection<PotMemberStatus> statuses);

    /**
     * 명세 '진행 중인 다른 방': 내 상태 JOINED·BOARDED·REQUESTED 이고,
     * 방이 RECRUITING·CLOSED 이거나 (RUNNING 이면서 fare_claim_status = NONE)
     */
    @Query("""
            select p.id from TaxipotMember m, Taxipot p
            where m.potId = p.id
              and m.memberId = :memberId
              and m.status in :memberStatuses
              and (p.status in :openStatuses
                   or (p.status = :running and p.fareClaimStatus = :claimNone))
            """)
    List<Long> findActivePotIds(@Param("memberId") Long memberId,
                                @Param("memberStatuses") Collection<PotMemberStatus> memberStatuses,
                                @Param("openStatuses") Collection<PotStatus> openStatuses,
                                @Param("running") PotStatus running,
                                @Param("claimNone") FareClaimStatus claimNone);
}
