package com.taxipot.wallet.repository;

import com.taxipot.wallet.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 조회 메서드 추가는 wallet 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
