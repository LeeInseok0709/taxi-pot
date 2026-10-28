package com.taxipot.domain.admin.dto;

import java.util.List;

/**
 * StatisticsResponse — API 명세서 3.9.0 components.schemas.StatisticsResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record StatisticsResponse(
        Integer totalPots,
        Integer completedPots,
        Integer canceledPots,
        List<TopDeparture> topDepartures,
        List<TopDestination> topDestinations,
        List<HourlyTraffic> hourlyTraffic
) {

    public record TopDeparture(
            String name,
            Integer count
    ) {
    }

    public record TopDestination(
            String name,
            Integer count
    ) {
    }

    public record HourlyTraffic(
            Integer hour,
            Integer potCount
    ) {
    }
}
