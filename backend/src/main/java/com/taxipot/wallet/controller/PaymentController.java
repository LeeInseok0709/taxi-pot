package com.taxipot.wallet.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.wallet.dto.PaymentConfirmRequest;
import com.taxipot.wallet.dto.PaymentCreateRequest;
import com.taxipot.wallet.dto.PaymentResponse;
import com.taxipot.wallet.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 PaymentService에만 작성.
 */
@Tag(name = "Payment")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** POST /api/payments — 담당 효우 */
    @Operation(summary = "포인트 충전 요청")
    @PostMapping(value = "/api/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @LoginUserId Long userId,
            @Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(userId, request));
    }

    /** GET /api/payments — 담당 효우 */
    @Operation(summary = "내 충전 결제 내역 조회")
    @GetMapping(value = "/api/payments")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentPage(
            @LoginUserId Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.getPaymentPage(userId, status, PageRequests.of(page, size)));
    }

    /** POST /api/payments/confirm — 담당 효우 */
    @Operation(summary = "충전 결제 승인 (PORTONE 전용)")
    @PostMapping(value = "/api/payments/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @LoginUserId Long userId,
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(userId, request));
    }
}
