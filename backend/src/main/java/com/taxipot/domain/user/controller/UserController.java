package com.taxipot.domain.user.controller;

import com.taxipot.domain.auth.dto.TokenResponse;
import com.taxipot.domain.pot.service.MyPotHistoryService;
import com.taxipot.domain.user.dto.AccountResponse;
import com.taxipot.domain.user.dto.AccountUpdateRequest;
import com.taxipot.domain.user.dto.FcmTokenRequest;
import com.taxipot.domain.user.dto.MyPotHistoryPageResponse;
import com.taxipot.domain.user.dto.MyProfileResponse;
import com.taxipot.domain.user.dto.PasswordChangeRequest;
import com.taxipot.domain.user.dto.UserPublicResponse;
import com.taxipot.domain.user.dto.UserUpdateRequest;
import com.taxipot.domain.user.dto.WithdrawRequest;
import com.taxipot.domain.user.service.UserService;
import com.taxipot.domain.user.service.UserWithdrawFacade;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.security.LoginUserId;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: User — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "User")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserWithdrawFacade userWithdrawFacade;
    private final MyPotHistoryService myPotHistoryService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/api/users/me")
    public MyProfileResponse getMyProfile(@LoginUserId Long userId) {
        return userService.getMyProfile(userId);
    }

    @Operation(summary = "내 정보 수정")
    @PatchMapping("/api/users/me")
    public MyProfileResponse updateMyProfile(@LoginUserId Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateMyProfile(userId, request);
    }

    @Operation(summary = "회원 탈퇴 (논리 삭제)")
    @PostMapping("/api/users/me/withdraw")
    public ResponseEntity<Void> withdraw(@LoginUserId Long userId, @Valid @RequestBody WithdrawRequest request) {
        userWithdrawFacade.withdraw(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/api/users/me/password")
    public TokenResponse changePassword(@LoginUserId Long userId, @Valid @RequestBody PasswordChangeRequest request) {
        return userService.changePassword(userId, request);
    }

    @Operation(summary = "내 출금 계좌 조회 (3.5.0)")
    @GetMapping("/api/users/me/account")
    public AccountResponse getMyAccount(@LoginUserId Long userId) {
        return userService.getMyAccount(userId);
    }

    @Operation(summary = "출금 계좌 등록·변경 (3.5.0)")
    @PutMapping("/api/users/me/account")
    public AccountResponse updateMyAccount(@LoginUserId Long userId, @Valid @RequestBody AccountUpdateRequest request) {
        return userService.updateMyAccount(userId, request);
    }

    @Operation(summary = "FCM 푸시 토큰 등록·갱신")
    @PutMapping("/api/users/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@LoginUserId Long userId, @Valid @RequestBody FcmTokenRequest request) {
        userService.updateFcmToken(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 택시 이용 기록 조회")
    @GetMapping("/api/users/me/pots")
    public MyPotHistoryPageResponse getMyPots(@LoginUserId Long userId, @RequestParam(required = false) PotMemberRole role, @RequestParam(required = false) PotStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return myPotHistoryService.getMyPots(userId, role, status, page, size);
    }

    @Operation(summary = "회원 공개 프로필 조회")
    @GetMapping("/api/users/{userId}")
    public UserPublicResponse getPublicProfile(@LoginUserId Long requesterId, @PathVariable Long userId) {
        return userService.getPublicProfile(requesterId, userId);
    }
}
