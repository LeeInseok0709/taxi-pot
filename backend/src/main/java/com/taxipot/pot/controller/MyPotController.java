package com.taxipot.pot.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.MyPotHistoryResponse;
import com.taxipot.pot.service.MyPotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 서연
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 MyPotService에만 작성.
 */
@Tag(name = "User")
@RestController
@RequiredArgsConstructor
public class MyPotController {

    private final MyPotService myPotService;

    /** GET /api/users/me/pots — 담당 서연 */
    @Operation(summary = "내 택시 이용 기록 조회")
    @GetMapping(value = "/api/users/me/pots")
    public ResponseEntity<PageResponse<MyPotHistoryResponse>> getMyPotHistoryPage(
            @LoginUserId Long userId,
            @RequestParam(required = false) PotMemberRole role,
            @RequestParam(required = false) PotStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(myPotService.getMyPotHistoryPage(userId, role, status, PageRequests.of(page, size)));
    }
}
