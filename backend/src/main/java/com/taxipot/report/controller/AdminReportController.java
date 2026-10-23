package com.taxipot.report.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.report.dto.AdminReportResponse;
import com.taxipot.report.dto.ReportProcessRequest;
import com.taxipot.report.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminReportService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    /** GET /api/admin/reports — 담당 서연 */
    @Operation(summary = "신고 목록 조회")
    @GetMapping(value = "/api/admin/reports")
    public ResponseEntity<PageResponse<AdminReportResponse>> getReportPage(
            @LoginUserId Long adminId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminReportService.getReportPage(adminId, status, type, PageRequests.of(page, size)));
    }

    /** PATCH /api/admin/reports/{reportId} — 담당 서연 */
    @Operation(summary = "신고 처리 (승인·반려)")
    @PatchMapping(value = "/api/admin/reports/{reportId}")
    public ResponseEntity<AdminReportResponse> processReport(
            @LoginUserId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request) {
        return ResponseEntity.ok(adminReportService.processReport(adminId, reportId, request));
    }
}
