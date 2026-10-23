package com.taxipot.report.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.report.dto.ReportCreateRequest;
import com.taxipot.report.dto.ReportResponse;
import com.taxipot.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 ReportService에만 작성.
 */
@Tag(name = "Report")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** GET /api/users/me/reports — 담당 서연 */
    @Operation(summary = "내가 접수한 신고 내역 조회")
    @GetMapping(value = "/api/users/me/reports")
    public ResponseEntity<PageResponse<ReportResponse>> getMyReportPage(
            @LoginUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getMyReportPage(userId, PageRequests.of(page, size)));
    }

    /** POST /api/reports — 담당 서연 */
    @Operation(summary = "신고 접수")
    @PostMapping(value = "/api/reports")
    public ResponseEntity<ReportResponse> createReport(
            @LoginUserId Long userId,
            @Valid @RequestBody ReportCreateRequest request) {
        return ResponseEntity.ok(reportService.createReport(userId, request));
    }
}
