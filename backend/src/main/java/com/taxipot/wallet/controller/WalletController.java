package com.taxipot.wallet.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.code.PointType;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.wallet.dto.DebtPayResponse;
import com.taxipot.wallet.dto.DebtResponse;
import com.taxipot.wallet.dto.PointHistoryResponse;
import com.taxipot.wallet.dto.WalletResponse;
import com.taxipot.wallet.dto.WithdrawalRequest;
import com.taxipot.wallet.dto.WithdrawalResponse;
import com.taxipot.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 WalletService에만 작성.
 */
@Tag(name = "Wallet")
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /** GET /api/wallet — 담당 효우 */
    @Operation(summary = "내 지갑 잔액 조회")
    @GetMapping(value = "/api/wallet")
    public ResponseEntity<WalletResponse> getMyWallet(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(walletService.getMyWallet(userId));
    }

    /** GET /api/wallet/history — 담당 효우 */
    @Operation(summary = "포인트 이용 내역 조회")
    @GetMapping(value = "/api/wallet/history")
    public ResponseEntity<PageResponse<PointHistoryResponse>> getPointHistoryPage(
            @LoginUserId Long userId,
            @RequestParam(required = false) PointType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(walletService.getPointHistoryPage(userId, type, PageRequests.of(page, size)));
    }

    /** GET /api/wallet/debts — 담당 효우 */
    @Operation(summary = "내 미수금 목록 조회 (3.3.0)")
    @GetMapping(value = "/api/wallet/debts")
    public ResponseEntity<List<DebtResponse>> getDebtList(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(walletService.getDebtList(userId));
    }

    /** POST /api/wallet/debts/{settlementId}/pay — 담당 효우 */
    @Operation(summary = "미수금 납부 (3.3.0)")
    @PostMapping(value = "/api/wallet/debts/{settlementId}/pay")
    public ResponseEntity<DebtPayResponse> payDebt(
            @LoginUserId Long userId,
            @PathVariable Long settlementId) {
        return ResponseEntity.ok(walletService.payDebt(userId, settlementId));
    }

    /** POST /api/wallet/withdrawals — 담당 효우 */
    @Operation(summary = "등록 계좌로 출금 (3.5.0)")
    @PostMapping(value = "/api/wallet/withdrawals")
    public ResponseEntity<WithdrawalResponse> createWithdrawal(
            @LoginUserId Long userId,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(walletService.createWithdrawal(userId, request));
    }

    /** GET /api/wallet/withdrawals — 담당 효우 */
    @Operation(summary = "내 출금 내역 조회 (3.5.0)")
    @GetMapping(value = "/api/wallet/withdrawals")
    public ResponseEntity<PageResponse<WithdrawalResponse>> getWithdrawalPage(
            @LoginUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(walletService.getWithdrawalPage(userId, PageRequests.of(page, size)));
    }
}
