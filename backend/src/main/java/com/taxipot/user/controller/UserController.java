package com.taxipot.user.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.user.dto.AccountResponse;
import com.taxipot.user.dto.AccountUpdateRequest;
import com.taxipot.user.dto.FcmTokenRequest;
import com.taxipot.user.dto.MyProfileResponse;
import com.taxipot.user.dto.PasswordChangeRequest;
import com.taxipot.user.dto.TokenResponse;
import com.taxipot.user.dto.UserPublicResponse;
import com.taxipot.user.dto.UserUpdateRequest;
import com.taxipot.user.dto.WithdrawRequest;
import com.taxipot.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 UserService에만 작성.
 */
@Tag(name = "User")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/me — 담당 기성 */
    @Operation(summary = "내 정보 조회")
    @GetMapping(value = "/api/users/me")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /** PATCH /api/users/me — 담당 기성 */
    @Operation(summary = "내 정보 수정")
    @PatchMapping(value = "/api/users/me")
    public ResponseEntity<MyProfileResponse> updateMyProfile(
            @LoginUserId Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(userId, request));
    }

    /** POST /api/users/me/withdraw — 담당 기성 */
    @Operation(summary = "회원 탈퇴 (논리 삭제)")
    @PostMapping(value = "/api/users/me/withdraw")
    public ResponseEntity<Void> withdraw(
            @LoginUserId Long userId,
            @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/users/me/password — 담당 기성 */
    @Operation(summary = "비밀번호 변경")
    @PatchMapping(value = "/api/users/me/password")
    public ResponseEntity<TokenResponse> changePassword(
            @LoginUserId Long userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(userService.changePassword(userId, request));
    }

    /** GET /api/users/me/account — 담당 기성 */
    @Operation(summary = "내 출금 계좌 조회 (3.5.0)")
    @GetMapping(value = "/api/users/me/account")
    public ResponseEntity<AccountResponse> getMyAccount(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(userService.getMyAccount(userId));
    }

    /** PUT /api/users/me/account — 담당 기성 */
    @Operation(summary = "출금 계좌 등록·변경 (3.5.0)")
    @PutMapping(value = "/api/users/me/account")
    public ResponseEntity<AccountResponse> updateMyAccount(
            @LoginUserId Long userId,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyAccount(userId, request));
    }

    /** PUT /api/users/me/fcm-token — 담당 기성 */
    @Operation(summary = "FCM 푸시 토큰 등록·갱신")
    @PutMapping(value = "/api/users/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @LoginUserId Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        userService.updateFcmToken(userId, request);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/users/{userId} — 담당 기성 */
    @Operation(summary = "회원 공개 프로필 조회")
    @GetMapping(value = "/api/users/{userId}")
    public ResponseEntity<UserPublicResponse> getUserProfile(
            @LoginUserId Long userId,
            @PathVariable("userId") Long targetUserId) {
        return ResponseEntity.ok(userService.getUserProfile(userId, targetUserId));
    }
}
