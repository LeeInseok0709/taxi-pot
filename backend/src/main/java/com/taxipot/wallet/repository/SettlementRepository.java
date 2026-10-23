package com.taxipot.wallet.repository;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.wallet.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 조회 메서드 추가는 wallet 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByPotIdAndMemberId(Long potId, Long memberId);

    List<Settlement> findByMemberIdAndDebtStatusIn(Long memberId, Collection<DebtStatus> debtStatuses);
}
