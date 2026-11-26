package com.taxipot.domain.wallet.repository;

import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.global.code.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findAllByMemberId(Long memberId, Pageable pageable);

    Page<PointHistory> findAllByMemberIdAndType(Long memberId, PointType type, Pageable pageable);
}
