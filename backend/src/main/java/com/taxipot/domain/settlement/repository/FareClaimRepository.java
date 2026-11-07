package com.taxipot.domain.settlement.repository;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.global.code.FareClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * 실요금 청구 목록 조회 전용 (운영자 GET /api/admin/fare-claims). 담당: 지갑·정산
 * <p>TAXIPOT 테이블을 읽기만 한다. TaxipotRepository(택시팟 담당 파일)를 고치지 않으려고 정산 패키지에 따로 둔다.
 */
public interface FareClaimRepository extends Repository<Taxipot, Long> {

    Page<Taxipot> findAllByFareClaimStatus(FareClaimStatus fareClaimStatus, Pageable pageable);
}
