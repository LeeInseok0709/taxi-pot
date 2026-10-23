package com.taxipot.settlement.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.global.file.StoredImage;
import com.taxipot.settlement.dto.BatchStatusRequest;
import com.taxipot.settlement.dto.FareClaimResponse;
import com.taxipot.settlement.dto.FareClaimReviewRequest;
import com.taxipot.settlement.service.AdminFareClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminFareClaimService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminFareClaimController {

    private final AdminFareClaimService adminFareClaimService;

    /** GET /api/admin/fare-claims — 담당 효우 */
    @Operation(summary = "실요금 청구 목록 (운영자, 3.5.0)")
    @GetMapping(value = "/api/admin/fare-claims")
    public ResponseEntity<PageResponse<FareClaimResponse>> getFareClaimPage(
            @LoginUserId Long adminId,
            @RequestParam(required = false) FareClaimStatus fareClaimStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminFareClaimService.getFareClaimPage(adminId, fareClaimStatus, PageRequests.of(page, size)));
    }

    /** PATCH /api/admin/pots/{potId}/fare-claim — 담당 효우 */
    @Operation(summary = "실요금 청구 승인·반려 (운영자, 3.5.0)")
    @PatchMapping(value = "/api/admin/pots/{potId}/fare-claim")
    public ResponseEntity<FareClaimResponse> reviewFareClaim(
            @LoginUserId Long adminId,
            @PathVariable Long potId,
            @Valid @RequestBody FareClaimReviewRequest request) {
        return ResponseEntity.ok(adminFareClaimService.reviewFareClaim(adminId, potId, request));
    }

    /** GET /api/admin/pots/{potId}/receipt — 담당 효우 */
    @Operation(summary = "결제내역(영수증) 사진 조회 (운영자, 3.3.0)")
    @GetMapping(value = "/api/admin/pots/{potId}/receipt")
    public ResponseEntity<Resource> getReceiptImage(
            @LoginUserId Long adminId,
            @PathVariable Long potId) {
        StoredImage image = adminFareClaimService.getReceiptImage(adminId, potId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.resource());
    }

    /** POST /api/admin/pots/batch-status — 담당 효우 */
    @Operation(summary = "장애 복구 후 상태 일괄 입력 (선택 기능)")
    @PostMapping(value = "/api/admin/pots/batch-status")
    public ResponseEntity<Void> updateBatchStatus(
            @LoginUserId Long adminId,
            @Valid @RequestBody BatchStatusRequest request) {
        adminFareClaimService.updateBatchStatus(adminId, request);
        return ResponseEntity.noContent().build();
    }
}
