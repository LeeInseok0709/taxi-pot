package com.taxipot.domain.admin.service;

import com.taxipot.domain.admin.dto.StatisticsResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /**
     * <b>GET /api/admin/statistics</b> — 매칭 현황 통계 (주요 출발지·선호 목적지·시간대별)
     * <p>
     * TAXIPOT 집계 쿼리로 산출 (CANCELED 제외). 기간 미지정 시 최근 30일
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(Long adminId, LocalDate from, LocalDate to, int limit) {
        // TODO(회원·운영): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/statistics");
    }
}
