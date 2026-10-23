package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.MyPotHistoryResponse;
import com.taxipot.pot.dto.PotSummaryResponse;
import com.taxipot.pot.dto.StatisticsResponse;
import com.taxipot.pot.service.AdminPotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminPotService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminPotController {

    private final AdminPotService adminPotService;

    /** GET /api/admin/users/{userId}/pots — 담당 서연 */
    @Operation(summary = "회원 이용 기록 조회")
    @GetMapping(value = "/api/admin/users/{userId}/pots")
    public ResponseEntity<PageResponse<MyPotHistoryResponse>> getUserPotHistoryPage(
            @LoginUserId Long adminId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPotService.getUserPotHistoryPage(adminId, userId, PageRequests.of(page, size)));
    }

    /** GET /api/admin/pots — 담당 서연 */
    @Operation(summary = "전체 택시팟 이용 기록 조회")
    @GetMapping(value = "/api/admin/pots")
    public ResponseEntity<PageResponse<PotSummaryResponse>> getPotPage(
            @LoginUserId Long adminId,
            @RequestParam(required = false) PotStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPotService.getPotPage(adminId, status, from, to, PageRequests.of(page, size)));
    }

    /** GET /api/admin/statistics — 담당 서연 */
    @Operation(summary = "매칭 현황 통계 (주요 출발지·선호 목적지·시간대별)")
    @GetMapping(value = "/api/admin/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
            @LoginUserId Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(adminPotService.getStatistics(adminId, from, to, limit));
    }
}
