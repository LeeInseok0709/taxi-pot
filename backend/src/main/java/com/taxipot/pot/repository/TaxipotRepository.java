package com.taxipot.pot.repository;

import com.taxipot.pot.entity.Taxipot;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 조회 메서드 추가는 pot 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface TaxipotRepository extends JpaRepository<Taxipot, Long> {

    /** 락 순서 3번 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select p from Taxipot p where p.id = :id")
    Optional<Taxipot> findByIdForUpdate(@Param("id") Long id);
}
