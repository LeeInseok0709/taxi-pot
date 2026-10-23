package com.taxipot.wallet.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.wallet.dto.WalletAdjustRequest;
import com.taxipot.wallet.dto.WalletResponse;
import com.taxipot.wallet.service.AdminWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 AdminWalletService에만 작성.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    /** PATCH /api/admin/users/{userId}/wallet — 담당 효우 */
    @Operation(summary = "지갑 잔액 조정 (시연 충전 수단, 3.7.0 확정)")
    @PatchMapping(value = "/api/admin/users/{userId}/wallet")
    public ResponseEntity<WalletResponse> adjustWallet(
            @LoginUserId Long adminId,
            @PathVariable Long userId,
            @Valid @RequestBody WalletAdjustRequest request) {
        return ResponseEntity.ok(adminWalletService.adjustWallet(adminId, userId, request));
    }
}
