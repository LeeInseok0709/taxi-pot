package com.taxipot.wallet.service;

import com.taxipot.global.code.PointType;
import com.taxipot.global.common.PageResponse;
import com.taxipot.wallet.dto.DebtPayResponse;
import com.taxipot.wallet.dto.DebtResponse;
import com.taxipot.wallet.dto.PointHistoryResponse;
import com.taxipot.wallet.dto.WalletResponse;
import com.taxipot.wallet.dto.WithdrawalRequest;
import com.taxipot.wallet.dto.WithdrawalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * WalletController 전용 API 서비스. 담당: 효우
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    /**
     * GET /api/wallet — 내 지갑 잔액 조회 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 메인 화면 상시 표시용
     */
    public WalletResponse getMyWallet(Long userId) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/wallet");
    }

    /**
     * GET /api/wallet/history — 포인트 이용 내역 조회 (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * POINT_HISTORY 최신순 (충전·예치·환불·택시비·수수료·방장 정산금·미수금 납부·잔액 소멸).
     * 정산 1건은 같은 시각에 여러 행(FARE_REFUND 예치 해제 → FARE_PAY → FEE, 방장은 HOST_PAYOUT 포함)으로 기록되므로 id 내림차순으로 정렬한다
     */
    public PageResponse<PointHistoryResponse> getPointHistoryPage(Long userId, PointType type, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/wallet/history");
    }

    /**
     * GET /api/wallet/debts — 내 미수금 목록 조회 (3.3.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * SETTLEMENT에서 debt_status가 UNPAID·OVERDUE인 행 (납부 기한 빠른 순). 메인 화면에 WalletResponse.debtAmount > 0이면 배너로 안내
     */
    public List<DebtResponse> getDebtList(Long userId) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/wallet/debts");
    }

    /**
     * POST /api/wallet/debts/{settlementId}/pay — 미수금 납부 (3.3.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 지갑 사용 가능 잔액(balance)으로 미수금을 납부한다. Body 없음. 락 순서: USERS(본인) → WALLET(본인) → SETTLEMENT 행
     * - 납부액 = min(balance, debt_amount). balance가 0이면 409 INSUFFICIENT_BALANCE (detail: requiredAmount = debt_amount)
     * - POINT_HISTORY DEBT_PAY(−납부액, pot_id = 해당 방), debt_amount − 납부액
     * - debt_amount가 0이 되면 debt_status = PAID, debt_paid_at = now
     * - 본인 정산 행이 아니면 404, 미수금이 없으면(NONE·PAID) 409 NO_DEBT
     * - 기한이 지나도(OVERDUE) 납부 금액은 그대로이며 같은 방법으로 납부한다 (10배는 약관 문구만, 3.4.0)
     */
    @Transactional
    public DebtPayResponse payDebt(Long userId, Long settlementId) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/wallet/debts/{settlementId}/pay");
    }

    /**
     * POST /api/wallet/withdrawals — 등록 계좌로 출금 (3.5.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 지갑 사용 가능 잔액(balance)을 가입 시 등록한 본인 계좌로 보낸다. 락 순서: USERS(본인) → WALLET(본인)
     * - 검증: 계좌 등록(아니면 409 ACCOUNT_NOT_REGISTERED) → amount ≥ 1,000(아니면 400 INVALID_INPUT) → amount ≤ balance(아니면 409 INSUFFICIENT_BALANCE)
     *   - 미수금이 있으면 이용 제한으로 409 UNPAID_DEBT_EXISTS. 정지 중이어도 출금 가능. 예치금(held_balance)은 출금 대상 아님
     * - 1단계(트랜잭션 1): WITHDRAWAL 생성(status = REQUESTED, 계좌 스냅샷 복사), balance − amount, POINT_HISTORY WITHDRAW(−amount, withdrawal_id 연결) 커밋
     * - 2단계(송금): transfer_provider = DUMMY는 실제 송금 없이 즉시 성공 → transfer_tid = `DUMMY-{UUID}`, status = COMPLETED, completed_at = now,
     *   PAYMENT 알림('N원이 OO은행 ****7890 계좌로 입금되었습니다')
     *   - 실패 시 status = FAILED, fail_reason, completed_at = now, balance + amount, POINT_HISTORY WITHDRAW_REFUND(+amount, 같은 withdrawal_id), 실패 알림
     *   - 시연에서 실패를 보이려면 설정값 `app.withdrawal.dummy-fail-amount`와 같은 금액으로 요청
     * - 응답은 2단계 결과(COMPLETED 또는 FAILED)
     */
    @Transactional
    public WithdrawalResponse createWithdrawal(Long userId, WithdrawalRequest request) {
        throw new UnsupportedOperationException("TODO(효우): POST /api/wallet/withdrawals");
    }

    /**
     * GET /api/wallet/withdrawals — 내 출금 내역 조회 (3.5.0) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 최신순 페이지. 계좌번호는 마스킹
     */
    public PageResponse<WithdrawalResponse> getWithdrawalPage(Long userId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(효우): GET /api/wallet/withdrawals");
    }
}
