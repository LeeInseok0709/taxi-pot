package com.taxipot.domain.pot.service;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 택시팟 예치·환불 (SETTLEMENT 행 + WALLET 금액 + POINT_HISTORY 를 항상 함께 바꾼다).
 * <p>방 생성·참여·입장 요청 수락은 {@link #hold}. 환불·해산은 인석의 {@link PotExitHandler} (refundHold·dissolve)를 쓴다.
 * <p>호출 전에 MemberWalletLocker 로 USERS → WALLET, 그다음 TAXIPOT 을 잠가 둔다 (CONVENTIONS 8장).
 */
@Component
@RequiredArgsConstructor
public class PotDeposit {

    private final SettlementRepository settlementRepository;
    private final WalletLedger walletLedger;

    /**
     * 예치: SETTLEMENT 를 HELD 로 만들고(처음이면 INSERT, 환불 이력이 있으면 같은 행 rehold — UNIQUE(pot_id, member_id))
     * WALLET balance → held_balance, POINT_HISTORY FARE_HOLD(−amount).
     * 잔액 부족이면 WalletLedger 가 409 INSUFFICIENT_BALANCE.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void hold(Wallet wallet, Long potId, int amount) {
        Long memberId = wallet.getMemberId();
        Settlement settlement = settlementRepository.findByPotIdAndMemberId(potId, memberId).orElse(null);
        if (settlement == null) {
            settlementRepository.save(Settlement.hold(potId, memberId, amount));
        } else if (settlement.getStatus() == SettlementStatus.REFUNDED) {
            settlement.rehold(amount);
        } else {
            // HELD·SETTLED 가 남아 있는데 다시 예치하려는 경우 = 이미 참여 중인 방 (이중 예치 방지)
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
        walletLedger.hold(wallet, potId, amount);
    }
}
