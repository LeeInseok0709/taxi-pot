package com.taxipot.user.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.global.file.StoredImage;
import com.taxipot.user.dto.AdminVerificationResponse;
import com.taxipot.user.dto.VerificationReviewRequest;
import com.taxipot.user.service.AdminVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminVerificationService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    /** GET /api/admin/verifications — 담당 기성 */
    @Operation(summary = "학생 인증 심사 목록 조회")
    @GetMapping(value = "/api/admin/verifications")
    public ResponseEntity<PageResponse<AdminVerificationResponse>> getVerificationPage(
            @LoginUserId Long adminId,
            @RequestParam(required = false) ReviewStatus reviewStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminVerificationService.getVerificationPage(adminId, reviewStatus, PageRequests.of(page, size)));
    }

    /** GET /api/admin/verifications/{verificationId} — 담당 기성 */
    @Operation(summary = "학생 인증 상세 조회 (캡처본 열람)")
    @GetMapping(value = "/api/admin/verifications/{verificationId}")
    public ResponseEntity<AdminVerificationResponse> getVerification(
            @LoginUserId Long adminId,
            @PathVariable Long verificationId) {
        return ResponseEntity.ok(adminVerificationService.getVerification(adminId, verificationId));
    }

    /** PATCH /api/admin/verifications/{verificationId} — 담당 기성 */
    @Operation(summary = "학생 인증 심사 (승인·반려)")
    @PatchMapping(value = "/api/admin/verifications/{verificationId}")
    public ResponseEntity<AdminVerificationResponse> reviewVerification(
            @LoginUserId Long adminId,
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationReviewRequest request) {
        return ResponseEntity.ok(adminVerificationService.reviewVerification(adminId, verificationId, request));
    }

    /** GET /api/admin/verifications/{verificationId}/capture — 담당 기성 */
    @Operation(summary = "학생 인증 캡처 이미지 조회 (3.3.0)")
    @GetMapping(value = "/api/admin/verifications/{verificationId}/capture")
    public ResponseEntity<Resource> getCaptureImage(
            @LoginUserId Long adminId,
            @PathVariable Long verificationId) {
        StoredImage image = adminVerificationService.getCaptureImage(adminId, verificationId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.resource());
    }
}
