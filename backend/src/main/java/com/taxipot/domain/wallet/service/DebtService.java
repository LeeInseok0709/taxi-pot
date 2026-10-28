package com.taxipot.domain.wallet.service;

import com.taxipot.domain.wallet.dto.DebtPayResponse;
import com.taxipot.domain.wallet.dto.DebtResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class DebtService {

    /**
     * <b>GET /api/wallet/debts</b> — 내 미수금 목록 조회 (3.3.0)
     * <p>
     * SETTLEMENT에서 debt_status가 UNPAID·OVERDUE인 행 (납부 기한 빠른 순). 메인 화면에 WalletResponse.debtAmount > 0이면 배너로 안내
     */
    @Transactional(readOnly = true)
    public List<DebtResponse> getMyDebts(Long userId) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/wallet/debts");
    }

    /**
     * <b>POST /api/wallet/debts/{settlementId}/pay</b> — 미수금 납부 (3.3.0)
     * <p>
     * 지갑 사용 가능 잔액(balance)으로 미수금을 납부한다. Body 없음. 락 순서: USERS(본인) → WALLET(본인) → SETTLEMENT 행
     * - 납부액 = min(balance, debt_amount). balance가 0이면 409 INSUFFICIENT_BALANCE (detail: requiredAmount = debt_amount)
     * - POINT_HISTORY DEBT_PAY(−납부액, pot_id = 해당 방), debt_amount − 납부액
     * - debt_amount가 0이 되면 debt_status = PAID, debt_paid_at = now
     * - 본인 정산 행이 아니면 404, 미수금이 없으면(NONE·PAID) 409 NO_DEBT
     * - 기한이 지나도(OVERDUE) 납부 금액은 그대로이며 같은 방법으로 납부한다 (10배는 약관 문구만, 3.4.0)
     * <p>
     * <b>에러 코드</b>
     * - 404: NOT_FOUND
     * - 409: NO_DEBT / INSUFFICIENT_BALANCE
     */
    @Transactional
    public DebtPayResponse payDebt(Long userId, Long settlementId) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/wallet/debts/{settlementId}/pay");
    }
}
