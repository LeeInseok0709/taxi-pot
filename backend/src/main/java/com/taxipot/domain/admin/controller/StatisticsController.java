package com.taxipot.domain.admin.controller;

import com.taxipot.domain.admin.dto.StatisticsResponse;
import com.taxipot.domain.admin.service.StatisticsService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 명세 tag: Admin — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "매칭 현황 통계 (주요 출발지·선호 목적지·시간대별)")
    @GetMapping("/api/admin/statistics")
    public StatisticsResponse getStatistics(@LoginUserId Long adminId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "10") int limit) {
        return statisticsService.getStatistics(adminId, from, to, limit);
    }
}
