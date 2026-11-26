package com.taxipot.domain.verification.controller;

import com.taxipot.domain.verification.dto.CaptureSubmitRequest;
import com.taxipot.domain.verification.dto.EmailCodeConfirmRequest;
import com.taxipot.domain.verification.dto.EmailCodeSendResponse;
import com.taxipot.domain.verification.dto.VerificationResponse;
import com.taxipot.domain.verification.service.VerificationService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Verification — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Verification")
@RestController
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "이메일 인증코드 재발송")
    @PostMapping("/api/verifications/email-code")
    public EmailCodeSendResponse resendEmailCode(@LoginUserId Long userId) {
        return verificationService.resendEmailCode(userId);
    }

    @Operation(summary = "이메일 인증코드 확인 (1차 인증)")
    @PostMapping("/api/verifications/email-code/confirm")
    public VerificationResponse confirmEmailCode(@LoginUserId Long userId, @Valid @RequestBody EmailCodeConfirmRequest request) {
        return verificationService.confirmEmailCode(userId, request);
    }

    @Operation(summary = "학생 기본 정보 캡처본 제출 (2차 인증, 반려 후 재제출 포함)")
    @PostMapping(value = "/api/verifications/capture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VerificationResponse submitCapture(@LoginUserId Long userId, @Valid @ModelAttribute CaptureSubmitRequest request) {
        return verificationService.submitCapture(userId, request);
    }

    @Operation(summary = "내 인증 진행 상태 조회")
    @GetMapping("/api/verifications/me")
    public VerificationResponse getMyVerification(@LoginUserId Long userId) {
        return verificationService.getMyVerification(userId);
    }
}
