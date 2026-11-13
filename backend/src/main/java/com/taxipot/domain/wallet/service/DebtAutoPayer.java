package com.taxipot.domain.wallet.service;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미수금 자동 납부 (명세 3.5.0 '충전 승인과 같은 규칙'). 담당: 지갑·정산
 * <p>입금(+ 운영자 잔액 조정, PG 충전 승인) 직후 같은 트랜잭션에서 호출한다.
 * 사용 가능 잔액(balance)으로 미수금을 납부 기한 빠른 순서대로 갚는다.
 * <ul>
 *   <li>한 건 납부액 = min(balance, debt_amount) → 원장 DEBT_PAY(−납부액, pot_id = 해당 방)</li>
 *   <li>debt_amount가 0이 되면 PAID (Settlement.payDebt 가 처리)</li>
 *   <li>잔액이 0이 되면 멈춘다 (남은 미수금은 그대로)</li>
 * </ul>
 * 호출 전에 MemberWalletLocker 로 회원 → 지갑을 잠가 둔 상태여야 한다.
 */
@Component
@RequiredArgsConstructor
public class DebtAutoPayer {

    private static final List<DebtStatus> DEBT_STATUSES = List.of(DebtStatus.UNPAID, DebtStatus.OVERDUE);

    private final SettlementRepository settlementRepository;
    private final WalletLedger walletLedger;

    /** @return 이번에 납부한 총액 (없으면 0) */
    @Transactional(propagation = Propagation.MANDATORY)
    public int payAll(Wallet wallet, LocalDateTime now) {
        List<Settlement> debts = settlementRepository
                .findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(wallet.getMemberId(), DEBT_STATUSES);

        int paidTotal = 0;
        for (Settlement debt : debts) {
            int pay = Math.min(wallet.getBalance(), debt.getDebtAmount());
            if (pay <= 0) {
                break;  // 잔액을 다 썼으면 멈춘다
            }
            walletLedger.debit(wallet, PointType.DEBT_PAY, pay, debt.getPotId(), null);
            debt.payDebt(pay, now);
            paidTotal += pay;
        }
        return paidTotal;
    }
}
