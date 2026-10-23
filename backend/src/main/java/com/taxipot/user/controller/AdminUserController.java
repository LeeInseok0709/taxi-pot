package com.taxipot.user.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.user.dto.AdminUserDetailResponse;
import com.taxipot.user.dto.AdminUserResponse;
import com.taxipot.user.dto.UserStatusUpdateRequest;
import com.taxipot.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminUserService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** GET /api/admin/users — 담당 기성 */
    @Operation(summary = "가입 회원 목록 조회")
    @GetMapping(value = "/api/admin/users")
    public ResponseEntity<PageResponse<AdminUserResponse>> getUserPage(
            @LoginUserId Long adminId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.getUserPage(adminId, keyword, status, PageRequests.of(page, size)));
    }

    /** GET /api/admin/users/{userId} — 담당 기성 */
    @Operation(summary = "회원 상세 조회 (개인정보·지갑·캡처본·신고 이력 포함)")
    @GetMapping(value = "/api/admin/users/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(
            @LoginUserId Long adminId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(adminId, userId));
    }

    /** PATCH /api/admin/users/{userId}/status — 담당 기성 */
    @Operation(summary = "회원 이용 정지·해제 (수동)")
    @PatchMapping(value = "/api/admin/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @LoginUserId Long adminId,
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(adminId, userId, request));
    }
}
