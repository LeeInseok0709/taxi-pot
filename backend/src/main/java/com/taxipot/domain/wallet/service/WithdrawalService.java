package com.taxipot.domain.wallet.service;

import com.taxipot.domain.auth.catalog.BankCatalog;
import com.taxipot.domain.auth.dto.BankResponse;
import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.dto.WithdrawalPageResponse;
import com.taxipot.domain.wallet.dto.WithdrawalRequest;
import com.taxipot.domain.wallet.dto.WithdrawalResponse;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.entity.Withdrawal;
import com.taxipot.domain.wallet.repository.WithdrawalRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PointType;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.util.Texts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final MemberWalletLocker memberWalletLocker;
    private final WalletLedger walletLedger;
    private final WithdrawalRepository withdrawalRepository;
    private final NotificationSender notificationSender;
    private final AppProperties props;
    private final TimeProvider timeProvider;

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
     * 구현 메모: 시연은 DUMMY 송금이라 2단계가 즉시 끝나므로 한 트랜잭션 안에서 1단계 → 2단계를 순서대로 처리한다.
     * 결과(출금 행 상태, WITHDRAW → 실패 시 WITHDRAW_REFUND 원장, 잔액)는 명세와 같다.
     * 실제 은행 송금을 붙일 때는 1단계를 별도 트랜잭션으로 먼저 커밋하도록 나눠야 한다.
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / INVALID_DATETIME_FORMAT
     * - 409: ACCOUNT_NOT_REGISTERED / INSUFFICIENT_BALANCE / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public WithdrawalResponse requestWithdrawal(Long userId, WithdrawalRequest request) {
        int amount = request.amount();

        // 락: USERS(본인) → WALLET(본인)
        LockedMember me = memberWalletLocker.lock(userId);
        User user = me.user();
        Wallet wallet = me.wallet();

        // ---- 검증 (명세 순서 그대로) ----
        if (!user.hasAccount()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_REGISTERED);
        }
        int minAmount = props.withdrawal().minAmount();
        if (amount < minAmount) {
            throw Errors.invalidInput("출금은 %,d원 이상부터 가능합니다.".formatted(minAmount));
        }
        if (amount > wallet.getBalance()) {   // 예치금(held_balance)은 출금 대상이 아니라 balance만 본다
            throw Errors.insufficientBalance(amount, wallet.getBalance());
        }
        // (미수금이 있으면 DebtRestrictionInterceptor 가 이 API 전에 409 UNPAID_DEBT_EXISTS 로 막는다)

        // ---- 1단계: 출금 신청 기록 + 잔액 차감 ----
        Withdrawal withdrawal = withdrawalRepository.save(Withdrawal.request(
                userId, amount, user.getBankCode(), user.getAccountNo(), user.getAccountHolder()));   // 계좌 스냅샷
        walletLedger.debit(wallet, PointType.WITHDRAW, amount, null, withdrawal.getId());

        // ---- 2단계: 더미 송금 ----
        LocalDateTime now = timeProvider.now();
        if (isDummyFailure(amount)) {
            withdrawal.fail("더미 송금 실패 (시연용 실패 금액)", now);
            walletLedger.credit(wallet, PointType.WITHDRAW_REFUND, amount, null, null, withdrawal.getId(), false);
            notificationSender.send(userId, null, NotificationType.PAYMENT, "출금 실패",
                    "%,d원 출금에 실패해 지갑으로 다시 돌려드렸습니다.".formatted(amount));
        } else {
            withdrawal.complete("DUMMY-" + UUID.randomUUID(), now);
            notificationSender.send(userId, null, NotificationType.PAYMENT, "출금 완료",
                    "%,d원이 %s %s 계좌로 입금되었습니다.".formatted(
                            amount, bankName(withdrawal.getBankCode()), Texts.maskAccount(withdrawal.getAccountNo())));
        }

        return toResponse(withdrawal, wallet.getBalance());
    }

    /**
     * <b>GET /api/wallet/withdrawals</b> — 내 출금 내역 조회 (3.5.0)
     * <p>
     * 최신순 페이지. 계좌번호는 마스킹
     */
    @Transactional(readOnly = true)
    public WithdrawalPageResponse getMyWithdrawals(Long userId, int page, int size) {
        Page<Withdrawal> result = withdrawalRepository.findAllByMemberId(
                userId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new WithdrawalPageResponse(
                result.getContent().stream().map(w -> toResponse(w, null)).toList(),   // 내역 조회에서는 balanceAfter = null
                PageMeta.from(result));
    }

    // ===================================================================== 내부 도우미

    /** 시연용: 설정한 금액과 같으면 송금 실패로 처리 (0이면 사용 안 함) */
    private boolean isDummyFailure(int amount) {
        int failAmount = props.withdrawal().dummyFailAmount();
        return failAmount > 0 && amount == failAmount;
    }

    /** 은행 코드 → 은행 이름 (탈퇴 후 파기돼 코드가 없으면 null) */
    private static String bankName(String bankCode) {
        if (bankCode == null) {
            return null;
        }
        return BankCatalog.BANKS.stream()
                .filter(b -> b.code().equals(bankCode))
                .map(BankResponse::name)
                .findFirst()
                .orElse(bankCode);
    }

    private static WithdrawalResponse toResponse(Withdrawal w, Integer balanceAfter) {
        return new WithdrawalResponse(
                w.getId(),
                w.getAmount(),
                bankName(w.getBankCode()),
                Texts.maskAccount(w.getAccountNo()),
                w.getAccountHolder(),
                w.getTransferProvider().name(),
                w.getTransferTid(),
                w.getStatus(),
                w.getFailReason(),
                balanceAfter,
                Kst.toOffset(w.getRequestedAt()),
                Kst.toOffset(w.getCompletedAt()));
    }
}
