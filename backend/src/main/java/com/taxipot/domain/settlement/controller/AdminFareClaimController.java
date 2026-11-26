package com.taxipot.domain.settlement.controller;

import com.taxipot.domain.settlement.dto.BatchStatusRequest;
import com.taxipot.domain.settlement.dto.FareClaimPageResponse;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.settlement.dto.FareClaimReviewRequest;
import com.taxipot.domain.settlement.service.AdminBatchStatusService;
import com.taxipot.domain.settlement.service.AdminFareClaimService;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.file.ImageResponses;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
public class AdminFareClaimController {

    private final AdminFareClaimService adminFareClaimService;
    private final AdminBatchStatusService adminBatchStatusService;

    @Operation(summary = "실요금 청구 목록 (운영자, 3.5.0)")
    @GetMapping("/api/admin/fare-claims")
    public FareClaimPageResponse getFareClaims(@LoginUserId Long adminId, @RequestParam(required = false) FareClaimStatus fareClaimStatus, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminFareClaimService.getFareClaims(adminId, fareClaimStatus, page, size);
    }

    @Operation(summary = "실요금 청구 승인·반려 (운영자, 3.5.0)")
    @PatchMapping("/api/admin/pots/{potId}/fare-claim")
    public FareClaimResponse reviewFareClaim(@LoginUserId Long adminId, @PathVariable Long potId, @Valid @RequestBody FareClaimReviewRequest request) {
        return adminFareClaimService.reviewFareClaim(adminId, potId, request);
    }

    @Operation(summary = "결제내역(영수증) 사진 조회 (운영자, 3.3.0)")
    @GetMapping("/api/admin/pots/{potId}/receipt")
    public ResponseEntity<Resource> getReceipt(@LoginUserId Long adminId, @PathVariable Long potId) {
        return ImageResponses.of(adminFareClaimService.getReceipt(adminId, potId));
    }

    @Operation(summary = "장애 복구 후 상태 일괄 입력 (선택 기능)")
    @PostMapping("/api/admin/pots/batch-status")
    public ResponseEntity<Void> applyBatchStatus(@LoginUserId Long adminId, @Valid @RequestBody BatchStatusRequest request) {
        adminBatchStatusService.applyBatchStatus(adminId, request);
        return ResponseEntity.noContent().build();
    }
}
