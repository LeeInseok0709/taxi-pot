package com.taxipot.domain.payment.controller;

import com.taxipot.domain.payment.dto.PaymentConfirmRequest;
import com.taxipot.domain.payment.dto.PaymentCreateRequest;
import com.taxipot.domain.payment.dto.PaymentPageResponse;
import com.taxipot.domain.payment.dto.PaymentResponse;
import com.taxipot.domain.payment.service.PaymentService;
import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Payment — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Payment")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "포인트 충전 요청")
    @PostMapping("/api/payments")
    public PaymentResponse createPayment(@LoginUserId Long userId, @Valid @RequestBody PaymentCreateRequest request) {
        return paymentService.createPayment(userId, request);
    }

    @Operation(summary = "내 충전 결제 내역 조회")
    @GetMapping("/api/payments")
    public PaymentPageResponse getMyPayments(@LoginUserId Long userId, @RequestParam(required = false) PaymentStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return paymentService.getMyPayments(userId, status, page, size);
    }

    @Operation(summary = "충전 결제 승인 (PORTONE 전용)")
    @PostMapping("/api/payments/confirm")
    public PaymentResponse confirmPayment(@LoginUserId Long userId, @Valid @RequestBody PaymentConfirmRequest request) {
        return paymentService.confirmPayment(userId, request);
    }
}
