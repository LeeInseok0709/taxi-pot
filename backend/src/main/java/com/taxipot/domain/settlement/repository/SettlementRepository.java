package com.taxipot.domain.settlement.repository;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByPotIdAndMemberId(Long potId, Long memberId);

    List<Settlement> findAllByPotId(Long potId);

    List<Settlement> findAllByPotIdAndStatus(Long potId, SettlementStatus status);

    /** 미수금 목록 (UNPAID·OVERDUE), 납부 기한 빠른 순 = 자동 납부 순서 */
    List<Settlement> findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(Long memberId, Collection<DebtStatus> statuses);
}
