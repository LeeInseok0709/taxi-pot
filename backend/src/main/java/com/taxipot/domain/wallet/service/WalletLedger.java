package com.taxipot.domain.wallet.service;

import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.PointType;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 금액 변경 + POINT_HISTORY 기록을 항상 함께 하는 유일한 곳 (명세 '포인트 원장 기록 규칙').
 * <p>Wallet 은 MemberWalletLocker 로 잠근 것을 넘긴다. 금액이 0 인 거래는 기록하지 않는다.
 * <table>
 *   <tr><td>hold</td><td>FARE_HOLD(−예치액): 방 생성·참여·입장 요청 수락</td></tr>
 *   <tr><td>refundHold</td><td>FARE_REFUND(+예치액): 취소·퇴장·노쇼·방 취소, 정산 1단계 예치 해제</td></tr>
 *   <tr><td>credit</td><td>+: HOST_PAYOUT, CHARGE, ADMIN_ADJUST(+), WITHDRAW_REFUND</td></tr>
 *   <tr><td>debit</td><td>−: FARE_PAY, FEE, DEBT_PAY, WITHDRAW, ADMIN_ADJUST(−), BALANCE_EXPIRE</td></tr>
 * </table>
 */
@Component
@RequiredArgsConstructor
public class WalletLedger {

    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final AppProperties props;

    /** 가입 시 지갑 생성 (잔액 0) */
    @Transactional(propagation = Propagation.MANDATORY)
    public Wallet open(Long memberId) {
        return walletRepository.save(Wallet.open(memberId));
    }

    /** 예치: balance → held_balance. 잔액 부족이면 409 INSUFFICIENT_BALANCE(detail 포함) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void hold(Wallet wallet, Long potId, int amount) {
        wallet.hold(amount);
        record(wallet, PointType.FARE_HOLD, -amount, potId, null, null);
    }

    /** 예치 해제: held_balance → balance (FARE_REFUND) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundHold(Wallet wallet, Long potId, int amount) {
        wallet.release(amount);
        record(wallet, PointType.FARE_REFUND, amount, potId, null, null);
    }

    /**
     * 입금. checkLimit = true 면 balance + held_balance 상한(1,000,000) 검사 → 409 WALLET_LIMIT_EXCEEDED.
     * HOST_PAYOUT·WITHDRAW_REFUND 는 상한 검사를 하지 않는다 (checkLimit = false).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void credit(Wallet wallet, PointType type, int amount, Long potId, Long paymentId, Long withdrawalId,
                       boolean checkLimit) {
        if (amount <= 0) {
            return;
        }
        int max = props.wallet().maxBalance();
        if (checkLimit && wallet.total() + amount > max) {
            throw Errors.walletLimitExceeded(max, max - wallet.total());
        }
        wallet.deposit(amount);
        record(wallet, type, amount, potId, paymentId, withdrawalId);
    }

    /** 출금성 차감. 잔액 부족이면 409 INSUFFICIENT_BALANCE (부분 차감이 필요하면 호출 측에서 min 계산) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void debit(Wallet wallet, PointType type, int amount, Long potId, Long withdrawalId) {
        if (amount <= 0) {
            return;
        }
        wallet.deduct(amount);
        record(wallet, type, -amount, potId, null, withdrawalId);
    }

    private void record(Wallet wallet, PointType type, int signedAmount, Long potId, Long paymentId, Long withdrawalId) {
        pointHistoryRepository.save(PointHistory.of(wallet.getMemberId(), type, signedAmount, wallet.getBalance(),
                potId, paymentId, withdrawalId));
    }
}
