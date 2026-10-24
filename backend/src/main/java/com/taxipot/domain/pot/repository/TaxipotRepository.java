package com.taxipot.domain.pot.repository;

import com.taxipot.domain.pot.entity.Taxipot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaxipotRepository extends JpaRepository<Taxipot, Long>, JpaSpecificationExecutor<Taxipot> {

    /** 락 순서 3단계: TAXIPOT (USERS → WALLET 을 먼저 잠근 뒤) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Taxipot p where p.id = :id")
    Optional<Taxipot> findByIdForUpdate(@Param("id") Long id);
}
