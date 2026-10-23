package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.PotCreateRequest;
import com.taxipot.pot.dto.PotDetailResponse;
import com.taxipot.pot.dto.PotSort;
import com.taxipot.pot.dto.PotSummaryResponse;
import com.taxipot.pot.dto.PotUpdateRequest;
import com.taxipot.pot.service.PotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 PotService에만 작성.
 */
@Tag(name = "Pot")
@RestController
@RequiredArgsConstructor
public class PotController {

    private final PotService potService;

    /** GET /api/pots — 담당 서연 */
    @Operation(summary = "택시팟 목록 조회 (지도 마커 / 바텀 시트 공용)")
    @GetMapping(value = "/api/pots")
    public ResponseEntity<PageResponse<PotSummaryResponse>> getPotPage(
            @LoginUserId Long userId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "1000") Integer radius,
            @RequestParam(required = false) String departureName,
            @RequestParam(required = false) String destinationName,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) GenderOption genderOption,
            @RequestParam(required = false) @DecimalMin("1") @DecimalMax("10") Double minHostRating,
            @RequestParam(defaultValue = "DEPARTURE_TIME") PotSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(potService.getPotPage(userId, lat, lng, radius, departureName, destinationName, from, to, genderOption, minHostRating, sort, PageRequests.of(page, size)));
    }

    /** POST /api/pots — 담당 서연 */
    @Operation(summary = "택시팟 방 생성")
    @PostMapping(value = "/api/pots")
    public ResponseEntity<PotDetailResponse> createPot(
            @LoginUserId Long userId,
            @Valid @RequestBody PotCreateRequest request) {
        return ResponseEntity.ok(potService.createPot(userId, request));
    }

    /** GET /api/pots/{potId} — 담당 서연 */
    @Operation(summary = "택시팟 상세 조회")
    @GetMapping(value = "/api/pots/{potId}")
    public ResponseEntity<PotDetailResponse> getPotDetail(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        return ResponseEntity.ok(potService.getPotDetail(userId, potId));
    }

    /** PATCH /api/pots/{potId} — 담당 서연 */
    @Operation(summary = "택시팟 정보 수정 (방장 전용)")
    @PatchMapping(value = "/api/pots/{potId}")
    public ResponseEntity<PotDetailResponse> updatePot(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody PotUpdateRequest request) {
        return ResponseEntity.ok(potService.updatePot(userId, potId, request));
    }

    /** DELETE /api/pots/{potId} — 담당 서연 */
    @Operation(summary = "택시팟 방 취소 (방장 전용)")
    @DeleteMapping(value = "/api/pots/{potId}")
    public ResponseEntity<Void> cancelPot(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        potService.cancelPot(userId, potId);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/pots/{potId}/start — 담당 서연 */
    @Operation(summary = "탑승 시작 (방장 전용)")
    @PostMapping(value = "/api/pots/{potId}/start")
    public ResponseEntity<PotDetailResponse> startPot(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        return ResponseEntity.ok(potService.startPot(userId, potId));
    }
}
