package com.taxipot.domain.report.controller;

import com.taxipot.domain.report.dto.ReportCreateRequest;
import com.taxipot.domain.report.dto.ReportPageResponse;
import com.taxipot.domain.report.dto.ReportResponse;
import com.taxipot.domain.report.service.ReportService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Report — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Report")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "내가 접수한 신고 내역 조회")
    @GetMapping("/api/users/me/reports")
    public ReportPageResponse getMyReports(@LoginUserId Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reportService.getMyReports(userId, page, size);
    }

    @Operation(summary = "신고 접수")
    @PostMapping("/api/reports")
    public ReportResponse createReport(@LoginUserId Long userId, @Valid @RequestBody ReportCreateRequest request) {
        return reportService.createReport(userId, request);
    }
}
