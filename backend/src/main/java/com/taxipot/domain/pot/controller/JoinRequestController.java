package com.taxipot.domain.pot.controller;

import com.taxipot.domain.pot.dto.JoinRequestCreateRequest;
import com.taxipot.domain.pot.dto.JoinRequestResponse;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.domain.pot.service.JoinRequestService;
import com.taxipot.global.security.LoginUserId;
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
 * 명세 tag: PotMember — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "PotMember")
@RestController
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @Operation(summary = "입장 요청 (참여 마감 후, 3.3.0)")
    @PostMapping("/api/pots/{potId}/join-requests")
    public JoinRequestResponse createJoinRequest(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody JoinRequestCreateRequest request) {
        return joinRequestService.createJoinRequest(userId, potId, request);
    }

    @Operation(summary = "입장 요청 대기 목록 조회 (방장 전용, 3.3.0)")
    @GetMapping("/api/pots/{potId}/join-requests")
    public List<JoinRequestResponse> getJoinRequests(@LoginUserId Long userId, @PathVariable Long potId) {
        return joinRequestService.getJoinRequests(userId, potId);
    }

    @Operation(summary = "입장 요청 철회 (3.3.0)")
    @DeleteMapping("/api/pots/{potId}/join-requests/me")
    public ResponseEntity<Void> withdrawJoinRequest(@LoginUserId Long userId, @PathVariable Long potId) {
        joinRequestService.withdrawJoinRequest(userId, potId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "입장 요청 수락 (방장 전용, 3.3.0)")
    @PostMapping("/api/pots/{potId}/join-requests/{userId}/accept")
    public JoinResponse acceptJoinRequest(@LoginUserId Long requesterId, @PathVariable Long potId, @PathVariable Long userId) {
        return joinRequestService.acceptJoinRequest(requesterId, potId, userId);
    }

    @Operation(summary = "입장 요청 거절 (방장 전용, 3.3.0)")
    @PostMapping("/api/pots/{potId}/join-requests/{userId}/reject")
    public ResponseEntity<Void> rejectJoinRequest(@LoginUserId Long requesterId, @PathVariable Long potId, @PathVariable Long userId) {
        joinRequestService.rejectJoinRequest(requesterId, potId, userId);
        return ResponseEntity.noContent().build();
    }
}
