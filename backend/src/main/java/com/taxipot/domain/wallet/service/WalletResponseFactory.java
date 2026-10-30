package com.taxipot.domain.wallet.service;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wallet → WalletResponse 변환. 담당: 지갑·정산
 * <p>GET /api/wallet(내 지갑)과 PATCH /api/admin/users/{userId}/wallet(운영자 조정)이 같은 응답을 쓰므로 한 곳에 둔다.
 * debtAmount = UNPAID·OVERDUE 행의 debt_amount 합 (명세 WalletResponse.debtAmount)
 */
@Component
@RequiredArgsConstructor
public class WalletResponseFactory {

    private static final List<DebtStatus> DEBT_STATUSES = List.of(DebtStatus.UNPAID, DebtStatus.OVERDUE);

    private final SettlementRepository settlementRepository;
    private final WalletRepository walletRepository;

    public WalletResponse of(Wallet wallet) {
        // 금액을 바꾼 뒤라면 DB에 먼저 반영해 updated_at(@PreUpdate)이 최신 시각이 되게 한다
        walletRepository.flush();

        int debtAmount = settlementRepository
                .findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(wallet.getMemberId(), DEBT_STATUSES)
                .stream()
                .filter(Settlement::hasDebt)
                .mapToInt(Settlement::getDebtAmount)
                .sum();

        return new WalletResponse(
                wallet.getId(),
                wallet.getMemberId(),
                wallet.getBalance(),
                debtAmount,
                wallet.getHeldBalance(),
                Kst.toOffset(wallet.getUpdatedAt()));
    }
}
