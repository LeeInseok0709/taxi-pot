package com.taxipot.domain.wallet.controller;

import com.taxipot.domain.wallet.dto.WalletAdjustRequest;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.service.AdminWalletService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Admin — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    @Operation(summary = "지갑 잔액 조정 (시연 충전 수단, 3.7.0 확정)")
    @PatchMapping("/api/admin/users/{userId}/wallet")
    public WalletResponse adjustWallet(@LoginUserId Long adminId, @PathVariable Long userId, @Valid @RequestBody WalletAdjustRequest request) {
        return adminWalletService.adjustWallet(adminId, userId, request);
    }
}
