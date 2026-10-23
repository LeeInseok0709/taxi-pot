package com.taxipot.user.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.user.dto.EmailCodeConfirmRequest;
import com.taxipot.user.dto.EmailCodeSendResponse;
import com.taxipot.user.dto.VerificationResponse;
import com.taxipot.user.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 VerificationService에만 작성.
 */
@Tag(name = "Verification")
@RestController
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /** POST /api/verifications/email-code — 담당 기성 */
    @Operation(summary = "이메일 인증코드 재발송")
    @PostMapping(value = "/api/verifications/email-code")
    public ResponseEntity<EmailCodeSendResponse> sendEmailCode(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(verificationService.sendEmailCode(userId));
    }

    /** POST /api/verifications/email-code/confirm — 담당 기성 */
    @Operation(summary = "이메일 인증코드 확인 (1차 인증)")
    @PostMapping(value = "/api/verifications/email-code/confirm")
    public ResponseEntity<VerificationResponse> confirmEmailCode(
            @LoginUserId Long userId,
            @Valid @RequestBody EmailCodeConfirmRequest request) {
        return ResponseEntity.ok(verificationService.confirmEmailCode(userId, request));
    }

    /** POST /api/verifications/capture — 담당 기성 */
    @Operation(summary = "학생 기본 정보 캡처본 제출 (2차 인증, 반려 후 재제출 포함)")
    @PostMapping(value = "/api/verifications/capture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> submitCapture(
            @LoginUserId Long userId,
            @RequestPart("captureImage") MultipartFile captureImage,
            @RequestParam("studentNo") @NotBlank @Size(max = 20) String studentNo) {
        return ResponseEntity.ok(verificationService.submitCapture(userId, captureImage, studentNo));
    }

    /** GET /api/verifications/me — 담당 기성 */
    @Operation(summary = "내 인증 진행 상태 조회")
    @GetMapping(value = "/api/verifications/me")
    public ResponseEntity<VerificationResponse> getMyVerification(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(verificationService.getMyVerification(userId));
    }
}
