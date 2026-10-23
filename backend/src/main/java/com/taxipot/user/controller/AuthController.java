package com.taxipot.user.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.user.dto.AvailabilityResponse;
import com.taxipot.user.dto.LoginRequest;
import com.taxipot.user.dto.LoginResponse;
import com.taxipot.user.dto.RefreshRequest;
import com.taxipot.user.dto.SignupRequest;
import com.taxipot.user.dto.TokenResponse;
import com.taxipot.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AuthService에만 작성.
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/signup — 담당 기성 */
    @Operation(summary = "회원가입")
    @PostMapping(value = "/api/auth/signup")
    public ResponseEntity<LoginResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /** GET /api/auth/nickname-check — 담당 기성 */
    @Operation(summary = "닉네임 사용 가능 여부 확인")
    @GetMapping(value = "/api/auth/nickname-check")
    public ResponseEntity<AvailabilityResponse> checkNicknameAvailability(
            @RequestParam @Size(max = 30) String nickname) {
        return ResponseEntity.ok(authService.checkNicknameAvailability(nickname));
    }

    /** POST /api/auth/login — 담당 기성 */
    @Operation(summary = "로그인 (학생 앱·운영자 웹 공용)")
    @PostMapping(value = "/api/auth/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** POST /api/auth/refresh — 담당 기성 */
    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping(value = "/api/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** POST /api/auth/logout — 담당 기성 */
    @Operation(summary = "로그아웃")
    @PostMapping(value = "/api/auth/logout")
    public ResponseEntity<Void> logout(
            @LoginUserId Long userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
