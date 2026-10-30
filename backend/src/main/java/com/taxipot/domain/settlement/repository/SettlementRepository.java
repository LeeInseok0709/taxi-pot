package com.taxipot.domain.settlement.repository;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByPotIdAndMemberId(Long potId, Long memberId);

    List<Settlement> findAllByPotId(Long potId);

    List<Settlement> findAllByPotIdAndStatus(Long potId, SettlementStatus status);

    /** 미수금 목록 (UNPAID·OVERDUE), 납부 기한 빠른 순 = 자동 납부 순서 */
    List<Settlement> findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(Long memberId, Collection<DebtStatus> statuses);

    /** 미수금 납부: USERS → WALLET 을 잠근 뒤 3단계로 이 정산 행을 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Settlement s where s.id = :id")
    Optional<Settlement> findByIdForUpdate(@Param("id") Long id);
}
