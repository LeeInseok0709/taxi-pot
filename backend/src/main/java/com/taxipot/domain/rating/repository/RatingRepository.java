package com.taxipot.domain.rating.repository;

import com.taxipot.domain.rating.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findAllByPotIdAndReviewerId(Long potId, Long reviewerId);

    boolean existsByPotIdAndReviewerIdAndRevieweeId(Long potId, Long reviewerId, Long revieweeId);

    /**
     * 방의 정산 완료 시각 = 그 방 SETTLEMENT.settled_at 의 최댓값 (운영자 청구 승인 시각, 명세 3.5.0). 정산 전이면 null.
     * <p>평가 제출 마감(RatingService)과 스케줄러 7 기본값(findPotIdsMissingRatings)이 반드시 같은 기준을 써야 한다.
     * 기준이 다르면 'API 는 아직 제출 가능한데 스케줄러가 먼저 ★3을 넣는' 틈이 생긴다.
     */
    @Query("select max(s.settledAt) from Settlement s where s.potId = :potId")
    LocalDateTime findSettledAt(@Param("potId") Long potId);

    /** 방의 평가 행 전체 (스케줄러 7: 이미 있는 평가쌍을 빼고 기본값을 넣기 위해) */
    List<Rating> findAllByPotId(Long potId);

    /**
     * 스케줄러 7 (평가 미제출 기본값) 대상 방 id (락 없음, 오름차순).
     * <ul>
     *   <li>방이 COMPLETED 이고, 그 방 정산 완료 시각(SETTLEMENT.settled_at 의 최댓값 = 운영자 청구 승인 시각)이 기준(now − 24시간) 이전</li>
     *   <li>BOARDED 탑승자끼리의 평가쌍(평가자 ≠ 피평가자) 중 RATING 행이 없는 쌍이 하나라도 남아 있음</li>
     * </ul>
     * 모든 쌍이 채워진 방은 자동으로 빠지므로 같은 방을 매시간 다시 처리하지 않는다.
     */
    @Query("select p.id from Taxipot p "
            + "where p.status = com.taxipot.global.code.PotStatus.COMPLETED "
            + "and (select max(s.settledAt) from Settlement s where s.potId = p.id) <= :threshold "
            + "and exists (select a.id from TaxipotMember a, TaxipotMember b "
            + "            where a.potId = p.id and b.potId = p.id "
            + "            and a.status = com.taxipot.global.code.PotMemberStatus.BOARDED "
            + "            and b.status = com.taxipot.global.code.PotMemberStatus.BOARDED "
            + "            and a.memberId <> b.memberId "
            + "            and not exists (select r.id from Rating r "
            + "                            where r.potId = p.id and r.reviewerId = a.memberId "
            + "                            and r.revieweeId = b.memberId)) "
            + "order by p.id asc")
    List<Long> findPotIdsMissingRatings(@Param("threshold") LocalDateTime threshold);
}
