package com.taxipot.domain.report.controller;

import com.taxipot.domain.report.dto.AdminReportPageResponse;
import com.taxipot.domain.report.dto.AdminReportResponse;
import com.taxipot.domain.report.dto.ReportProcessRequest;
import com.taxipot.domain.report.service.AdminReportService;
import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Admin — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고 목록 조회")
    @GetMapping("/api/admin/reports")
    public AdminReportPageResponse getReports(@LoginUserId Long adminId, @RequestParam(required = false) ReportStatus status, @RequestParam(required = false) ReportType type, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminReportService.getReports(adminId, status, type, page, size);
    }

    @Operation(summary = "신고 처리 (승인·반려)")
    @PatchMapping("/api/admin/reports/{reportId}")
    public AdminReportResponse processReport(@LoginUserId Long adminId, @PathVariable Long reportId, @Valid @RequestBody ReportProcessRequest request) {
        return adminReportService.processReport(adminId, reportId, request);
    }
}
