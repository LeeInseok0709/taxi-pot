package com.taxipot.domain.settlement.controller;

import com.taxipot.domain.settlement.dto.FareClaimRequest;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.settlement.dto.PotSettlementResponse;
import com.taxipot.domain.settlement.service.FareClaimService;
import com.taxipot.domain.settlement.service.SettlementQueryService;
import com.taxipot.global.file.ImageResponses;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Settlement — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Settlement")
@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final FareClaimService fareClaimService;
    private final SettlementQueryService settlementQueryService;

    @Operation(summary = "실요금 청구 (방장 전용, 3.5.0)")
    @PostMapping(value = "/api/pots/{potId}/fare-claim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FareClaimResponse claimFare(@LoginUserId Long userId, @PathVariable Long potId, @Valid @ModelAttribute FareClaimRequest request) {
        return fareClaimService.claimFare(userId, potId, request);
    }

    @Operation(summary = "택시팟 정산 내역 조회")
    @GetMapping("/api/pots/{potId}/settlement")
    public PotSettlementResponse getPotSettlement(@LoginUserId Long userId, @PathVariable Long potId) {
        return settlementQueryService.getPotSettlement(userId, potId);
    }

    @Operation(summary = "결제내역(영수증) 사진 조회 (3.3.0)")
    @GetMapping("/api/pots/{potId}/receipt")
    public ResponseEntity<Resource> getReceipt(@LoginUserId Long userId, @PathVariable Long potId) {
        return ImageResponses.of(settlementQueryService.getReceipt(userId, potId));
    }
}
