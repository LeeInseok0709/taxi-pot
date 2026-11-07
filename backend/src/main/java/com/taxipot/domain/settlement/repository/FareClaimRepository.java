package com.taxipot.domain.settlement.repository;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실요금 청구 목록 조회 전용 (운영자 GET /api/admin/fare-claims). 담당: 지갑·정산
 * <p>TAXIPOT 테이블을 읽기만 한다. TaxipotRepository(택시팟 담당 파일)를 고치지 않으려고 정산 패키지에 따로 둔다.
 */
public interface FareClaimRepository extends Repository<Taxipot, Long> {

    Page<Taxipot> findAllByFareClaimStatus(FareClaimStatus fareClaimStatus, Pageable pageable);

    /** 스케줄러 9: 운행 중인데 탑승 시작 후 기한(24시간)이 지나도록 청구가 없는 방 번호 */
    @Query("select t.id from Taxipot t where t.status = :status and t.fareClaimStatus = :claim and t.startedAt <= :threshold")
    List<Long> findClaimOverdueTargetIds(@Param("status") PotStatus status, @Param("claim") FareClaimStatus claim,
                                         @Param("threshold") LocalDateTime threshold);

    /** 스케줄러 10: 영수증이 남아 있는 방 중 ① 정산 승인 후 ② 취소 후 보관 기간(90일)이 지난 방 번호 */
    @Query("select t.id from Taxipot t where t.receiptImgPath is not null and ("
            + "(t.status = :completed and t.fareReviewedAt <= :threshold) or "
            + "(t.status = :canceled and t.updatedAt <= :threshold))")
    List<Long> findReceiptPurgeTargetIds(@Param("completed") PotStatus completed, @Param("canceled") PotStatus canceled,
                                         @Param("threshold") LocalDateTime threshold);
}
