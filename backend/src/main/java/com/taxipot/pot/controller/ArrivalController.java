package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.pot.dto.ArrivalRequest;
import com.taxipot.pot.dto.ArrivalResponse;
import com.taxipot.pot.service.ArrivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 인석
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 ArrivalService에만 작성.
 */
@Tag(name = "PotMember")
@RestController
@RequiredArgsConstructor
public class ArrivalController {

    private final ArrivalService arrivalService;

    /** POST /api/pots/{potId}/members/me/arrival — 담당 인석 */
    @Operation(summary = "도착 확인 (GPS 검증)")
    @PostMapping(value = "/api/pots/{potId}/members/me/arrival")
    public ResponseEntity<ArrivalResponse> confirmArrival(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody ArrivalRequest request) {
        return ResponseEntity.ok(arrivalService.confirmArrival(userId, potId, request));
    }
}
