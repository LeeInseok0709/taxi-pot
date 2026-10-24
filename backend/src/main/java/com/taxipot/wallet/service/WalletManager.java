package com.taxipot.wallet.service;

import com.taxipot.global.code.PointType;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.ErrorDetail;
import com.taxipot.wallet.entity.PointHistory;
import com.taxipot.wallet.entity.Settlement;
import com.taxipot.wallet.entity.Wallet;
import com.taxipot.wallet.repository.PointHistoryRepository;
import com.taxipot.wallet.repository.SettlementRepository;
import com.taxipot.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    private final SettlementRepository settlementRepository;
    private final PointHistoryRepository pointHistoryRepository;

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
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 0. 같은 방에 이미 예치 중(HELD)이거나 정산 끝난(SETTLED) 행이 있으면 중복 예치 → 거부
        //    REFUNDED(취소 이력)만 다시 예치할 수 있다
        Optional<Settlement> existing = settlementRepository.findByPotIdAndMemberId(potId, memberId);
        if (existing.isPresent() && existing.get().getStatus() != SettlementStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        // 지갑 가져오기 — 호출한 쪽이 같은 트랜잭션에서 이미 잠갔으므로 다시 잠가도 기다리지 않는다
        Wallet wallet = lockWallet(memberId);

        // 1. 잔액 부족 검사
        int balance = wallet.getBalance();
        if (balance < amount) {
            int shortage = amount - balance;
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                    "지갑 잔액이 %,d원 부족합니다.".formatted(shortage),
                    ErrorDetail.of("requiredAmount", amount, "balance", balance, "shortageAmount", shortage));
        }

        // 2. 지갑: 사용 가능 잔액 → 예치금
        wallet.hold(amount);

        // 3. 원장: FARE_HOLD, 금액은 음수(사용 가능 잔액이 줄었으므로)
        pointHistoryRepository.save(PointHistory.builder()
                .memberId(memberId)
                .potId(potId)
                .type(PointType.FARE_HOLD)
                .amount(-amount)
                .balanceAfter(wallet.getBalance())
                .build());

        // 4. 정산 행: 처음이면 새로 만들고, 취소 이력이 있으면 같은 행을 다시 HELD로
        existing.ifPresentOrElse(
                settlement -> settlement.rehold(amount),
                () -> settlementRepository.save(Settlement.builder()
                        .potId(potId)
                        .memberId(memberId)
                        .heldAmount(amount)
                        .build()));
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
        // 1. 예치 중(HELD)인 정산 행이 없으면 아무것도 안 함
        //    → 이미 환불했거나 정산이 끝난 경우 두 번 돌려주지 않는다 (이중 환불 방지)
        Settlement settlement = settlementRepository.findByPotIdAndMemberId(potId, memberId).orElse(null);
        if (settlement == null || settlement.getStatus() != SettlementStatus.HELD) {
            return;
        }

        int heldAmount = settlement.getHeldAmount();
        Wallet wallet = lockWallet(memberId);

        // 2. 지갑: 예치금 → 사용 가능 잔액
        wallet.releaseHold(heldAmount);

        // 3. 원장: FARE_REFUND, 금액은 양수(사용 가능 잔액이 늘었으므로)
        pointHistoryRepository.save(PointHistory.builder()
                .memberId(memberId)
                .potId(potId)
                .type(PointType.FARE_REFUND)
                .amount(heldAmount)
                .balanceAfter(wallet.getBalance())
                .build());

        // 4. 정산 행: REFUNDED
        settlement.refund();
    }
}
