package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.pot.dto.JoinRequestCreateRequest;
import com.taxipot.pot.dto.JoinRequestResponse;
import com.taxipot.pot.dto.JoinResponse;
import com.taxipot.pot.service.JoinRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 JoinRequestService에만 작성.
 */
@Tag(name = "PotMember")
@RestController
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    /** POST /api/pots/{potId}/join-requests — 담당 서연 */
    @Operation(summary = "입장 요청 (참여 마감 후, 3.3.0)")
    @PostMapping(value = "/api/pots/{potId}/join-requests")
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody JoinRequestCreateRequest request) {
        return ResponseEntity.ok(joinRequestService.createJoinRequest(userId, potId, request));
    }

    /** GET /api/pots/{potId}/join-requests — 담당 서연 */
    @Operation(summary = "입장 요청 대기 목록 조회 (방장 전용, 3.3.0)")
    @GetMapping(value = "/api/pots/{potId}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequestList(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        return ResponseEntity.ok(joinRequestService.getJoinRequestList(userId, potId));
    }

    /** DELETE /api/pots/{potId}/join-requests/me — 담당 서연 */
    @Operation(summary = "입장 요청 철회 (3.3.0)")
    @DeleteMapping(value = "/api/pots/{potId}/join-requests/me")
    public ResponseEntity<Void> cancelJoinRequest(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        joinRequestService.cancelJoinRequest(userId, potId);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/pots/{potId}/join-requests/{userId}/accept — 담당 서연 */
    @Operation(summary = "입장 요청 수락 (방장 전용, 3.3.0)")
    @PostMapping(value = "/api/pots/{potId}/join-requests/{userId}/accept")
    public ResponseEntity<JoinResponse> acceptJoinRequest(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @PathVariable("userId") Long targetUserId) {
        return ResponseEntity.ok(joinRequestService.acceptJoinRequest(userId, potId, targetUserId));
    }

    /** POST /api/pots/{potId}/join-requests/{userId}/reject — 담당 서연 */
    @Operation(summary = "입장 요청 거절 (방장 전용, 3.3.0)")
    @PostMapping(value = "/api/pots/{potId}/join-requests/{userId}/reject")
    public ResponseEntity<Void> rejectJoinRequest(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @PathVariable("userId") Long targetUserId) {
        joinRequestService.rejectJoinRequest(userId, potId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
