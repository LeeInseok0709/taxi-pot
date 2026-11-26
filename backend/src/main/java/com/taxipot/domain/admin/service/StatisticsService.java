package com.taxipot.domain.admin.service;

import com.taxipot.domain.admin.dto.StatisticsResponse;
import com.taxipot.domain.admin.repository.StatisticsRepository;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 담당: 회원·운영 — 운영자 통계 (인석 구현)
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** 기간 미지정 시 최근 30일 (오늘 포함) */
    private static final int DEFAULT_DAYS = 30;
    /** 순위 개수 상한 (명세 기본 10, 화면이 감당할 만큼만) */
    private static final int MAX_LIMIT = 50;

    private final StatisticsRepository statisticsRepository;
    private final TimeProvider timeProvider;

    /**
     * <b>GET /api/admin/statistics</b> — 매칭 현황 통계 (주요 출발지·선호 목적지·시간대별)
     * <p>
     * TAXIPOT 집계 쿼리로 산출 (CANCELED 제외). 기간 미지정 시 최근 30일
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
     */
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(Long adminId, LocalDate from, LocalDate to, int limit) {
        // 구현: 인석
        // 1. 기간: 출발일 기준, from·to 모두 포함. 하나만 오면 나머지는 30일 폭으로 채운다
        LocalDate today = timeProvider.now().toLocalDate();
        LocalDate end = to != null ? to : (from != null ? from.plusDays(DEFAULT_DAYS - 1) : today);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
        if (start.isAfter(end)) {
            throw Errors.invalidInput("from 은 to 보다 늦을 수 없습니다.");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw Errors.invalidInput("limit 는 1~" + MAX_LIMIT + " 사이여야 합니다.");
        }
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();   // to 날짜 하루 전체를 포함 (반열린 구간)

        // 2. 상태별 개수: totalPots 는 CANCELED 포함 전체, completed·canceled 는 각 상태 개수
        int total = 0;
        int completed = 0;
        int canceled = 0;
        for (Object[] row : statisticsRepository.countByStatus(fromTime, toTime)) {
            int count = ((Number) row[1]).intValue();
            total += count;
            if (row[0] == PotStatus.COMPLETED) {
                completed = count;
            } else if (row[0] == PotStatus.CANCELED) {
                canceled = count;
            }
        }

        // 3. 순위·시간대 (CANCELED 제외)
        PageRequest top = PageRequest.of(0, limit);
        List<StatisticsResponse.TopDeparture> departures = statisticsRepository.topDepartures(fromTime, toTime, top)
                .stream()
                .map(r -> new StatisticsResponse.TopDeparture((String) r[0], ((Number) r[1]).intValue()))
                .toList();
        List<StatisticsResponse.TopDestination> destinations = statisticsRepository.topDestinations(fromTime, toTime, top)
                .stream()
                .map(r -> new StatisticsResponse.TopDestination((String) r[0], ((Number) r[1]).intValue()))
                .toList();

        // 시간대는 0~23시 24칸을 모두 채워서 준다 (방이 없는 시간은 0) — 그래프를 그리기 쉽게
        int[] hourly = new int[24];
        for (Object[] row : statisticsRepository.countByHour(fromTime, toTime)) {
            hourly[((Number) row[0]).intValue()] = ((Number) row[1]).intValue();
        }
        List<StatisticsResponse.HourlyTraffic> traffic = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            traffic.add(new StatisticsResponse.HourlyTraffic(h, hourly[h]));
        }

        return new StatisticsResponse(total, completed, canceled, departures, destinations, traffic);
    }
}
