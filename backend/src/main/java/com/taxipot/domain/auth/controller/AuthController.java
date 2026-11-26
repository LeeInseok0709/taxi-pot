package com.taxipot.domain.auth.controller;

import com.taxipot.domain.auth.dto.AvailabilityResponse;
import com.taxipot.domain.auth.dto.BankResponse;
import com.taxipot.domain.auth.dto.LoginRequest;
import com.taxipot.domain.auth.dto.LoginResponse;
import com.taxipot.domain.auth.dto.RefreshRequest;
import com.taxipot.domain.auth.dto.SignupRequest;
import com.taxipot.domain.auth.dto.TokenResponse;
import com.taxipot.domain.auth.dto.UniversityResponse;
import com.taxipot.domain.auth.service.AuthService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 명세 tag: Auth — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/api/auth/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "닉네임 사용 가능 여부 확인")
    @GetMapping("/api/auth/nickname-check")
    public AvailabilityResponse checkNickname(@RequestParam @Size(max = 30) String nickname) {
        return authService.checkNickname(nickname);
    }

    @Operation(summary = "가입 가능 대학교 목록 조회 (3.3.0)")
    @GetMapping("/api/universities")
    public List<UniversityResponse> getUniversities() {
        return authService.getUniversities();
    }

    @Operation(summary = "출금 계좌 은행 목록 조회 (3.5.0)")
    @GetMapping("/api/banks")
    public List<BankResponse> getBanks() {
        return authService.getBanks();
    }

    @Operation(summary = "로그인 (학생 앱·운영자 웹 공용)")
    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping("/api/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@LoginUserId Long userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
