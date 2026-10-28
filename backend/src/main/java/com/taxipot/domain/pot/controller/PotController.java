package com.taxipot.domain.pot.controller;

import com.taxipot.domain.pot.dto.PotCreateRequest;
import com.taxipot.domain.pot.dto.PotDetailResponse;
import com.taxipot.domain.pot.dto.PotPageResponse;
import com.taxipot.domain.pot.dto.PotSort;
import com.taxipot.domain.pot.dto.PotUpdateRequest;
import com.taxipot.domain.pot.service.PotService;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
 * 명세 tag: Pot — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Pot")
@RestController
@RequiredArgsConstructor
@Validated
public class PotController {

    private final PotService potService;

    @Operation(summary = "택시팟 목록 조회 (지도 마커 / 바텀 시트 공용)")
    @GetMapping("/api/pots")
    public PotPageResponse getPots(@LoginUserId Long userId, @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng, @RequestParam(defaultValue = "1000") int radius, @RequestParam(required = false) String departureName, @RequestParam(required = false) String destinationName, @RequestParam(required = false) OffsetDateTime from, @RequestParam(required = false) OffsetDateTime to, @RequestParam(required = false) GenderOption genderOption, @RequestParam(required = false) @DecimalMin("1") @DecimalMax("10") Double minHostRating, @RequestParam(defaultValue = "DEPARTURE_TIME") PotSort sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return potService.getPots(userId, lat, lng, radius, departureName, destinationName, from, to, genderOption, minHostRating, sort, page, size);
    }

    @Operation(summary = "택시팟 방 생성")
    @PostMapping("/api/pots")
    public PotDetailResponse createPot(@LoginUserId Long userId, @Valid @RequestBody PotCreateRequest request) {
        return potService.createPot(userId, request);
    }

    @Operation(summary = "택시팟 상세 조회")
    @GetMapping("/api/pots/{potId}")
    public PotDetailResponse getPot(@LoginUserId Long userId, @PathVariable Long potId) {
        return potService.getPot(userId, potId);
    }

    @Operation(summary = "택시팟 정보 수정 (방장 전용)")
    @PatchMapping("/api/pots/{potId}")
    public PotDetailResponse updatePot(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody PotUpdateRequest request) {
        return potService.updatePot(userId, potId, request);
    }

    @Operation(summary = "택시팟 방 취소 (방장 전용)")
    @DeleteMapping("/api/pots/{potId}")
    public ResponseEntity<Void> cancelPot(@LoginUserId Long userId, @PathVariable Long potId) {
        potService.cancelPot(userId, potId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "탑승 시작 (방장 전용)")
    @PostMapping("/api/pots/{potId}/start")
    public PotDetailResponse startPot(@LoginUserId Long userId, @PathVariable Long potId) {
        return potService.startPot(userId, potId);
    }
}
