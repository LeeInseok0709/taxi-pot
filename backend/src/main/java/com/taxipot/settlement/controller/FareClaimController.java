package com.taxipot.settlement.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.file.StoredImage;
import com.taxipot.settlement.dto.FareClaimResponse;
import com.taxipot.settlement.dto.PotSettlementResponse;
import com.taxipot.settlement.service.FareClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 FareClaimService에만 작성.
 */
@Tag(name = "Settlement")
@RestController
@RequiredArgsConstructor
public class FareClaimController {

    private final FareClaimService fareClaimService;

    /** POST /api/pots/{potId}/fare-claim — 담당 효우 */
    @Operation(summary = "실요금 청구 (방장 전용, 3.5.0)")
    @PostMapping(value = "/api/pots/{potId}/fare-claim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FareClaimResponse> createFareClaim(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @RequestParam("actualFare") @Min(0) @Max(500000) Integer actualFare,
            @RequestPart("receiptImage") MultipartFile receiptImage) {
        return ResponseEntity.ok(fareClaimService.createFareClaim(userId, potId, actualFare, receiptImage));
    }

    /** GET /api/pots/{potId}/settlement — 담당 효우 */
    @Operation(summary = "택시팟 정산 내역 조회")
    @GetMapping(value = "/api/pots/{potId}/settlement")
    public ResponseEntity<PotSettlementResponse> getPotSettlement(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        return ResponseEntity.ok(fareClaimService.getPotSettlement(userId, potId));
    }

    /** GET /api/pots/{potId}/receipt — 담당 효우 */
    @Operation(summary = "결제내역(영수증) 사진 조회 (3.3.0)")
    @GetMapping(value = "/api/pots/{potId}/receipt")
    public ResponseEntity<Resource> getReceiptImage(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        StoredImage image = fareClaimService.getReceiptImage(userId, potId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.resource());
    }
}
