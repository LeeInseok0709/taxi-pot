package com.taxipot.domain.verification.controller;

import com.taxipot.domain.verification.dto.AdminVerificationPageResponse;
import com.taxipot.domain.verification.dto.AdminVerificationResponse;
import com.taxipot.domain.verification.dto.VerificationReviewRequest;
import com.taxipot.domain.verification.service.AdminVerificationService;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.file.ImageResponses;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @Operation(summary = "학생 인증 심사 목록 조회")
    @GetMapping("/api/admin/verifications")
    public AdminVerificationPageResponse getVerifications(@LoginUserId Long adminId, @RequestParam(required = false) ReviewStatus reviewStatus, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminVerificationService.getVerifications(adminId, reviewStatus, page, size);
    }

    @Operation(summary = "학생 인증 상세 조회 (캡처본 열람)")
    @GetMapping("/api/admin/verifications/{verificationId}")
    public AdminVerificationResponse getVerification(@LoginUserId Long adminId, @PathVariable Long verificationId) {
        return adminVerificationService.getVerification(adminId, verificationId);
    }

    @Operation(summary = "학생 인증 심사 (승인·반려)")
    @PatchMapping("/api/admin/verifications/{verificationId}")
    public AdminVerificationResponse reviewVerification(@LoginUserId Long adminId, @PathVariable Long verificationId, @Valid @RequestBody VerificationReviewRequest request) {
        return adminVerificationService.reviewVerification(adminId, verificationId, request);
    }

    @Operation(summary = "학생 인증 캡처 이미지 조회 (3.3.0)")
    @GetMapping("/api/admin/verifications/{verificationId}/capture")
    public ResponseEntity<Resource> getCaptureImage(@LoginUserId Long adminId, @PathVariable Long verificationId) {
        return ImageResponses.of(adminVerificationService.getCaptureImage(adminId, verificationId));
    }
}
