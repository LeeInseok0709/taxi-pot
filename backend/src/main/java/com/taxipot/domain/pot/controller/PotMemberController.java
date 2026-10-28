package com.taxipot.domain.pot.controller;

import com.taxipot.domain.pot.dto.ArrivalRequest;
import com.taxipot.domain.pot.dto.ArrivalResponse;
import com.taxipot.domain.pot.dto.JoinRequest;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.domain.pot.dto.PotMemberResponse;
import com.taxipot.domain.pot.dto.SeatChangeRequest;
import com.taxipot.domain.pot.service.PotMemberService;
import com.taxipot.global.security.LoginUserId;
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
 * 명세 tag: PotMember — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "PotMember")
@RestController
@RequiredArgsConstructor
public class PotMemberController {

    private final PotMemberService potMemberService;

    @Operation(summary = "택시팟 참여")
    @PostMapping("/api/pots/{potId}/members")
    public JoinResponse join(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody JoinRequest request) {
        return potMemberService.join(userId, potId, request);
    }

    @Operation(summary = "택시팟 참여 취소 (동승자 전용)")
    @DeleteMapping("/api/pots/{potId}/members/me")
    public ResponseEntity<Void> leave(@LoginUserId Long userId, @PathVariable Long potId) {
        potMemberService.leave(userId, potId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "희망 좌석 변경")
    @PatchMapping("/api/pots/{potId}/members/me/seat")
    public PotMemberResponse changeSeat(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody SeatChangeRequest request) {
        return potMemberService.changeSeat(userId, potId, request);
    }

    @Operation(summary = "도착 확인 (GPS 검증)")
    @PostMapping("/api/pots/{potId}/members/me/arrival")
    public ArrivalResponse confirmArrival(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody ArrivalRequest request) {
        return potMemberService.confirmArrival(userId, potId, request);
    }

    @Operation(summary = "미도착자 내보내기 (방장 전용)")
    @PostMapping("/api/pots/{potId}/members/{userId}/kick")
    public ResponseEntity<Void> kick(@LoginUserId Long requesterId, @PathVariable Long potId, @PathVariable Long userId) {
        potMemberService.kick(requesterId, potId, userId);
        return ResponseEntity.noContent().build();
    }
}
