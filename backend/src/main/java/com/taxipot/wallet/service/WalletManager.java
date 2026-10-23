package com.taxipot.wallet.service;

import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.wallet.entity.Wallet;
import com.taxipot.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/**
 * 지갑 공용 기능 (Manager). WALLET·POINT_HISTORY·SETTLEMENT 잔액 변경은 반드시 이 클래스를 거친다.
 * 시그니처는 확정 — 변경 금지. TODO 메서드 본문은 효우가 구현.
 * 주입 가능: UserManager, Repository. 주입 금지: PotManager, 모든 API 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletManager {

    private final WalletRepository walletRepository;
    private final AppProperties appProperties;

    /** 가입 시 지갑 생성 (balance 0, held 0) */
    @Transactional
    public void createWallet(Long memberId) {
        walletRepository.save(Wallet.builder().memberId(memberId).build());
    }

    public Wallet getWallet(Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 락 순서 2번 (WALLET). USERS를 먼저 잠근 뒤 호출 */
    @Transactional
    public Wallet lockWallet(Long memberId) {
        return walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 여러 회원: member_id 오름차순 */
    @Transactional
    public List<Wallet> lockWallets(Collection<Long> memberIds) {
        return walletRepository.findAllByMemberIdInForUpdate(memberIds);
    }

    /**
     * 1인 예치액 = floor(expected_fare × 1.2 ÷ max_members) + 수수료 1,000 (명세 3.7.0)
     * 예: 20,000원·4인 → 6,000 + 1,000 = 7,000
     */
    public int calculateHoldAmount(int expectedFare, int maxMembers) {
        BigDecimal share = BigDecimal.valueOf(expectedFare)
                .multiply(appProperties.pot().holdBufferRate())
                .divide(BigDecimal.valueOf(maxMembers), 0, RoundingMode.FLOOR);
        return share.intValue() + appProperties.pot().fee();
    }

    /**
     * 예치 (방 생성·참여·입장 요청 수락). 호출 전 USERS·WALLET 락 필수.
     * 1. balance < amount → 409 INSUFFICIENT_BALANCE
     *    message "지갑 잔액이 %,d원 부족합니다.", detail(requiredAmount, balance, shortageAmount)
     * 2. WALLET: balance − amount, held_balance + amount
     * 3. POINT_HISTORY: FARE_HOLD, amount = −amount, balance_after = 처리 후 balance, pot_id = potId
     * 4. SETTLEMENT: (pot_id, member_id) 행이 없으면 INSERT(HELD, held_amount = amount),
     *    취소 이력으로 행이 있으면 UPDATE (status HELD, held_amount = amount, fare_share·fee_amount·diff_amount·settled_at NULL,
     *    uncovered_amount 0, debt_status NONE) — 명세 POST /members '취소 이력이 있으면 UPDATE'
     */
    @Transactional
    public void hold(Long memberId, Long potId, int amount) {
        throw new UnsupportedOperationException("TODO(효우): WalletManager.hold");
    }

    /**
     * 예치금 전액 환불 (참여 취소·내보내기·노쇼·방 취소·자동 해산). 호출 전 USERS·WALLET 락 필수.
     * 1. SETTLEMENT(pot, member) 조회, status가 HELD가 아니면 아무것도 안 함 (이중 환불 방지)
     * 2. WALLET: held_balance − held_amount, balance + held_amount
     * 3. POINT_HISTORY: FARE_REFUND, amount = +held_amount
     * 4. SETTLEMENT: status REFUNDED, fare_share·fee_amount·diff_amount NULL
     */
    @Transactional
    public void refundHold(Long memberId, Long potId) {
        throw new UnsupportedOperationException("TODO(효우): WalletManager.refundHold");
    }
}
