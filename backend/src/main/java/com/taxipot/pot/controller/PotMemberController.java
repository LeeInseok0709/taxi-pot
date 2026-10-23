package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.pot.dto.JoinRequest;
import com.taxipot.pot.dto.JoinResponse;
import com.taxipot.pot.dto.PotMemberResponse;
import com.taxipot.pot.dto.SeatChangeRequest;
import com.taxipot.pot.service.PotMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 PotMemberService에만 작성.
 */
@Tag(name = "PotMember")
@RestController
@RequiredArgsConstructor
public class PotMemberController {

    private final PotMemberService potMemberService;

    /** POST /api/pots/{potId}/members — 담당 서연 */
    @Operation(summary = "택시팟 참여")
    @PostMapping(value = "/api/pots/{potId}/members")
    public ResponseEntity<JoinResponse> joinPot(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody(required = false) JoinRequest request) {
        return ResponseEntity.ok(potMemberService.joinPot(userId, potId, request));
    }

    /** DELETE /api/pots/{potId}/members/me — 담당 서연 */
    @Operation(summary = "택시팟 참여 취소 (동승자 전용)")
    @DeleteMapping(value = "/api/pots/{potId}/members/me")
    public ResponseEntity<Void> leavePot(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        potMemberService.leavePot(userId, potId);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/pots/{potId}/members/me/seat — 담당 서연 */
    @Operation(summary = "희망 좌석 변경")
    @PatchMapping(value = "/api/pots/{potId}/members/me/seat")
    public ResponseEntity<PotMemberResponse> changeSeat(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody SeatChangeRequest request) {
        return ResponseEntity.ok(potMemberService.changeSeat(userId, potId, request));
    }

    /** POST /api/pots/{potId}/members/{userId}/kick — 담당 서연 */
    @Operation(summary = "미도착자 내보내기 (방장 전용)")
    @PostMapping(value = "/api/pots/{potId}/members/{userId}/kick")
    public ResponseEntity<Void> kickMember(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @PathVariable("userId") Long targetUserId) {
        potMemberService.kickMember(userId, potId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
