package com.taxipot.domain.user.controller;

import com.taxipot.domain.pot.service.MyPotHistoryService;
import com.taxipot.domain.user.dto.AdminUserDetailResponse;
import com.taxipot.domain.user.dto.AdminUserPageResponse;
import com.taxipot.domain.user.dto.AdminUserResponse;
import com.taxipot.domain.user.dto.MyPotHistoryPageResponse;
import com.taxipot.domain.user.dto.UserStatusUpdateRequest;
import com.taxipot.domain.user.service.AdminUserService;
import com.taxipot.global.code.UserStatus;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Admin — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final MyPotHistoryService myPotHistoryService;

    @Operation(summary = "가입 회원 목록 조회")
    @GetMapping("/api/admin/users")
    public AdminUserPageResponse getUsers(@LoginUserId Long adminId, @RequestParam(required = false) String keyword, @RequestParam(required = false) UserStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminUserService.getUsers(adminId, keyword, status, page, size);
    }

    @Operation(summary = "회원 상세 조회 (개인정보·지갑·캡처본·신고 이력 포함)")
    @GetMapping("/api/admin/users/{userId}")
    public AdminUserDetailResponse getUserDetail(@LoginUserId Long adminId, @PathVariable Long userId) {
        return adminUserService.getUserDetail(adminId, userId);
    }

    @Operation(summary = "회원 이용 기록 조회")
    @GetMapping("/api/admin/users/{userId}/pots")
    public MyPotHistoryPageResponse getUserPotsForAdmin(@LoginUserId Long adminId, @PathVariable Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return myPotHistoryService.getUserPotsForAdmin(adminId, userId, page, size);
    }

    @Operation(summary = "회원 이용 정지·해제 (수동)")
    @PatchMapping("/api/admin/users/{userId}/status")
    public AdminUserResponse updateUserStatus(@LoginUserId Long adminId, @PathVariable Long userId, @Valid @RequestBody UserStatusUpdateRequest request) {
        return adminUserService.updateUserStatus(adminId, userId, request);
    }
}
