package com.taxipot.domain.user.service;

import com.taxipot.domain.payment.entity.Payment;
import com.taxipot.domain.payment.repository.PaymentRepository;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.dto.WithdrawRequest;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.code.PointType;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.web.UnpaidDebtQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 담당: 회원·운영
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawFacade {

    // Facade 는 여러 도메인을 함께 다루므로 다른 도메인 Repository·공용 컴포넌트를 직접 주입한다 (CONVENTIONS 5장)
    private final MemberWalletLocker memberWalletLocker;
    private final WalletLedger walletLedger;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final UnpaidDebtQuery unpaidDebtQuery;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    /**
     * <b>POST /api/users/me/withdraw</b> — 회원 탈퇴 (논리 삭제)
     * <p>
     * 비밀번호 확인이 필요해 Body를 받으므로 DELETE가 아닌 POST로 설계 (3.2.0에서 `DELETE /api/users/me` → 변경).
     * status = WITHDRAWN, withdrawn_at = now, fcm_token = NULL, token_version + 1. 회원 행은 이용·정산 기록 보존용으로 남긴다.
     * - 검증 순서: 비밀번호 → agreeBalanceForfeit = true(아니면 400 BALANCE_FORFEIT_NOT_AGREED) → 진행 중인 참여·입장 요청·HELD 정산(409 ACTIVE_POT_EXISTS)
     *   → 미수금(UNPAID·OVERDUE, 409 UNPAID_DEBT_EXISTS — 이용 제한 인터셉터에서 먼저 막힘) → 결제 대기 충전 정리
     * - 결제 대기 충전 정리 (3.5.0, PORTONE 연동 시에만 해당 — 시연에서는 PENDING 결제가 생기지 않음): PAYMENT status = PENDING 건마다 포트원 결제 조회 → 결제 완료 상태면 PG 결제 취소, 어느 경우든 status = FAILED (포인트 지급 없음)
     *   - PG 조회·취소가 실패하면 탈퇴를 진행하지 않고 409 PAYMENT_CHECK_FAILED (앱은 잠시 후 재시도 안내)
     * - 앱은 탈퇴 화면에서 잔액이 있으면 '출금 후 탈퇴'를 먼저 안내 (`POST /api/wallet/withdrawals`, 3.5.0)
     * - 남은 지갑 잔액은 소멸 (3.3.0, 약관 명시): balance > 0이면 POINT_HISTORY BALANCE_EXPIRE(−balance), balance = 0
     *   - 앱은 탈퇴 확인 화면에 현재 잔액과 '남은 잔액은 소멸되며 환불되지 않습니다' 문구 + 동의 체크박스를 보여준다
     * - token_version + 1로 이 기기를 포함한 모든 기기의 토큰이 즉시 무효 (이후 요청은 401 TOKEN_REVOKED)
     * - 개인정보는 14일 후 배치(스케줄러 6번)로 파기
     * - 성공 후 앱은 저장된 토큰을 삭제하고 로그인 화면으로 이동
     * <p>
     * <b>에러 코드</b>
     * - 400: BALANCE_FORFEIT_NOT_AGREED
     * - 401: INVALID_CREDENTIALS (비밀번호 불일치)
     * - 409: ACTIVE_POT_EXISTS / UNPAID_DEBT_EXISTS / PAYMENT_CHECK_FAILED
     */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // 0. 락: USERS → WALLET 순서 (CONVENTIONS 8장). 탈퇴 도중 참여·충전이 끼어들지 못하게 한다
        LockedMember me = memberWalletLocker.lock(userId);
        User user = me.user();
        Wallet wallet = me.wallet();

        // 1. 비밀번호 확인 (401 INVALID_CREDENTIALS)
        if (user.getPwHash() == null || !passwordEncoder.matches(request.password(), user.getPwHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 잔액 소멸 동의 (true 만 허용, 400 BALANCE_FORFEIT_NOT_AGREED)
        if (!Boolean.TRUE.equals(request.agreeBalanceForfeit())) {
            throw new BusinessException(ErrorCode.BALANCE_FORFEIT_NOT_AGREED);
        }

        // 3. 진행 중인 참여·입장 요청 (409 ACTIVE_POT_EXISTS, detail.activePotId)
        List<Long> activePotIds = taxipotMemberRepository.findActivePotIds(userId);
        if (!activePotIds.isEmpty()) {
            throw Errors.activePotExists(activePotIds.get(0));
        }

        // 3-1. 아직 정산되지 않은 예치(HELD): 실요금 청구 후 정산 대기 중인 방 등
        settlementRepository.findFirstByMemberIdAndStatusOrderByIdDesc(userId, SettlementStatus.HELD)
                .ifPresent(held -> {
                    throw Errors.activePotExists(held.getPotId());
                });

        // 4. 미수금 (409 UNPAID_DEBT_EXISTS). 보통 이용 제한 인터셉터에서 먼저 막히지만 한 번 더 확인
        unpaidDebtQuery.findUnpaid(userId).ifPresent(debt -> {
            throw Errors.unpaidDebtExists(debt.totalAmount(), debt.earliestDueAt());
        });

        // 5. 결제 대기(PENDING) 충전 정리 → FAILED (포인트 지급 없음)
        //    TODO(지갑·정산): 포트원 연동 시 PG 결제 조회 → 결제 완료면 PG 취소. 조회·취소 실패면 409 PAYMENT_CHECK_FAILED
        //    시연(PG 미연동)에서는 PENDING 결제가 생기지 않으므로 상태만 정리한다
        for (Payment payment : paymentRepository.findAllByMemberIdAndStatus(userId, PaymentStatus.PENDING)) {
            payment.markFailed();
        }

        // 6. 남은 잔액 소멸: POINT_HISTORY BALANCE_EXPIRE(−balance), balance = 0
        int balance = wallet.getBalance();
        if (balance > 0) {
            walletLedger.debit(wallet, PointType.BALANCE_EXPIRE, balance, null, null);
        }

        // 7. 탈퇴 처리: status = WITHDRAWN, withdrawn_at = now, fcm_token = NULL, token_version + 1
        //    (모든 기기 토큰 즉시 무효. 개인정보는 14일 후 스케줄러 6번이 파기)
        user.withdraw(timeProvider.now());
    }
}
