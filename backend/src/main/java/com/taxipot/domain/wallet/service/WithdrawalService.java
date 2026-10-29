package com.taxipot.domain.wallet.service;

import com.taxipot.domain.wallet.dto.WithdrawalPageResponse;
import com.taxipot.domain.wallet.dto.WithdrawalRequest;
import com.taxipot.domain.wallet.dto.WithdrawalResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    /**
     * <b>POST /api/wallet/withdrawals</b> — 등록 계좌로 출금 (3.5.0)
     * <p>
     * 지갑 사용 가능 잔액(balance)을 가입 시 등록한 본인 계좌로 보낸다. 락 순서: USERS(본인) → WALLET(본인)
     * - 검증: 계좌 등록(아니면 409 ACCOUNT_NOT_REGISTERED) → amount ≥ 1,000(아니면 400 INVALID_INPUT) → amount ≤ balance(아니면 409 INSUFFICIENT_BALANCE)
     *   - 미수금이 있으면 이용 제한으로 409 UNPAID_DEBT_EXISTS. 정지 중이어도 출금 가능. 예치금(held_balance)은 출금 대상 아님
     * - 1단계(트랜잭션 1): WITHDRAWAL 생성(status = REQUESTED, 계좌 스냅샷 복사), balance − amount, POINT_HISTORY WITHDRAW(−amount, withdrawal_id 연결) 커밋
     * - 2단계(송금): transfer_provider = DUMMY는 실제 송금 없이 즉시 성공 → transfer_tid = `DUMMY-{UUID}`, status = COMPLETED, completed_at = now,
     *   PAYMENT 알림('N원이 OO은행 ****7890 계좌로 입금되었습니다')
     *   - 실패 시 status = FAILED, fail_reason, completed_at = now, balance + amount, POINT_HISTORY WITHDRAW_REFUND(+amount, 같은 withdrawal_id), 실패 알림
     *   - 시연에서 실패를 보이려면 설정값 `app.withdrawal.dummy-fail-amount`와 같은 금액으로 요청
     * - 응답은 2단계 결과(COMPLETED 또는 FAILED)
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / INVALID_DATETIME_FORMAT
     * - 409: ACCOUNT_NOT_REGISTERED / INSUFFICIENT_BALANCE / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public WithdrawalResponse requestWithdrawal(Long userId, WithdrawalRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/wallet/withdrawals");
    }

    /**
     * <b>GET /api/wallet/withdrawals</b> — 내 출금 내역 조회 (3.5.0)
     * <p>
     * 최신순 페이지. 계좌번호는 마스킹
     */
    @Transactional(readOnly = true)
    public WithdrawalPageResponse getMyWithdrawals(Long userId, int page, int size) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/wallet/withdrawals");
    }
}
