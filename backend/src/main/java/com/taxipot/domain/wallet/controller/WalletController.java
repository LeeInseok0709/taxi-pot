package com.taxipot.domain.wallet.controller;

import com.taxipot.domain.wallet.dto.DebtPayResponse;
import com.taxipot.domain.wallet.dto.DebtResponse;
import com.taxipot.domain.wallet.dto.PointHistoryPageResponse;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.dto.WithdrawalPageResponse;
import com.taxipot.domain.wallet.dto.WithdrawalRequest;
import com.taxipot.domain.wallet.dto.WithdrawalResponse;
import com.taxipot.domain.wallet.service.DebtService;
import com.taxipot.domain.wallet.service.WalletService;
import com.taxipot.domain.wallet.service.WithdrawalService;
import com.taxipot.global.code.PointType;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 명세 tag: Wallet — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Wallet")
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final DebtService debtService;
    private final WithdrawalService withdrawalService;

    @Operation(summary = "내 지갑 잔액 조회")
    @GetMapping("/api/wallet")
    public WalletResponse getWallet(@LoginUserId Long userId) {
        return walletService.getWallet(userId);
    }

    @Operation(summary = "포인트 이용 내역 조회")
    @GetMapping("/api/wallet/history")
    public PointHistoryPageResponse getHistory(@LoginUserId Long userId, @RequestParam(required = false) PointType type, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return walletService.getHistory(userId, type, page, size);
    }

    @Operation(summary = "내 미수금 목록 조회 (3.3.0)")
    @GetMapping("/api/wallet/debts")
    public List<DebtResponse> getMyDebts(@LoginUserId Long userId) {
        return debtService.getMyDebts(userId);
    }

    @Operation(summary = "미수금 납부 (3.3.0)")
    @PostMapping("/api/wallet/debts/{settlementId}/pay")
    public DebtPayResponse payDebt(@LoginUserId Long userId, @PathVariable Long settlementId) {
        return debtService.payDebt(userId, settlementId);
    }

    @Operation(summary = "등록 계좌로 출금 (3.5.0)")
    @PostMapping("/api/wallet/withdrawals")
    public WithdrawalResponse requestWithdrawal(@LoginUserId Long userId, @Valid @RequestBody WithdrawalRequest request) {
        return withdrawalService.requestWithdrawal(userId, request);
    }

    @Operation(summary = "내 출금 내역 조회 (3.5.0)")
    @GetMapping("/api/wallet/withdrawals")
    public WithdrawalPageResponse getMyWithdrawals(@LoginUserId Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return withdrawalService.getMyWithdrawals(userId, page, size);
    }
}
