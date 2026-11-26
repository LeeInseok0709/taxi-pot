package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PointType;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 정산 실행 (명세 PATCH /api/admin/pots/{potId}/fare-claim 승인 3~7단계). 담당: 지갑·정산
 * <p>운영자 청구 승인(AdminFareClaimService)과 장애 복구 일괄 입력(AdminBatchStatusService)이 같은 로직을 쓰므로 한 곳에 둔다.
 * (서비스끼리 서로 부르지 않는다는 CONVENTIONS 규칙 때문에 컴포넌트로 분리)
 * <p>호출 전 조건: 탑승자 전원을 MemberWalletLocker.lockAll 로 잠그고, 그 다음 TAXIPOT 을 잠근 상태
 */
@Component
@RequiredArgsConstructor
public class SettlementExecutor {

    private final SettlementRepository settlementRepository;
    private final WalletLedger walletLedger;
    private final NotificationSender notificationSender;
    private final AppProperties props;

    /**
     * @param pot        정산할 방 (방장은 boarded 안에 있어야 함)
     * @param actualFare 확정된 실요금
     * @param boarded    탑승자(BOARDED) 전원의 잠근 회원·지갑, member_id 오름차순
     * @param now        정산 시각 (= 승인 시각)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(Taxipot pot, int actualFare, Map<Long, LockedMember> boarded, LocalDateTime now) {
        Long potId = pot.getId();
        if (boarded.isEmpty() || !boarded.containsKey(pot.getHostId())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);   // 방장은 반드시 탑승자
        }

        int fareShare = actualFare / boarded.size();      // 1/N 내림, 끝수는 택시팟 부담
        int fee = props.pot().feeAmount();
        int charge = fareShare + fee;                      // 1인 청구액
        Map<Long, Settlement> settlements = settlementRepository.findAllByPotId(potId).stream()
                .collect(Collectors.toMap(Settlement::getMemberId, Function.identity()));

        // 3. 방장 정산금: 실요금 전액 입금 (지갑 상한 검사 안 함 — 이미 낸 택시비라 막으면 안 됨)
        walletLedger.credit(boarded.get(pot.getHostId()).wallet(), PointType.HOST_PAYOUT, actualFare,
                potId, null, null, false);

        // 4. 탑승자별 정산 (member_id 오름차순)
        for (Map.Entry<Long, LockedMember> entry : boarded.entrySet()) {
            Long memberId = entry.getKey();
            Wallet wallet = entry.getValue().wallet();
            User user = entry.getValue().user();
            Settlement settlement = settlements.get(memberId);
            if (settlement == null) {
                throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
            }
            int heldAmount = settlement.getHeldAmount();

            // 예치 해제 → 분담금 → 수수료 (잔액이 모자라면 있는 만큼만, 0원 줄은 WalletLedger 가 기록 안 함)
            walletLedger.refundHold(wallet, potId, heldAmount);
            int paidShare = Math.min(wallet.getBalance(), fareShare);
            walletLedger.debit(wallet, PointType.FARE_PAY, paidShare, potId, null);
            int paidFee = Math.min(wallet.getBalance(), fee);
            walletLedger.debit(wallet, PointType.FEE, paidFee, potId, null);

            int uncovered = charge - paidShare - paidFee;
            LocalDateTime debtDueAt = uncovered > 0 ? now.plusDays(props.debt().dueDays()) : null;
            settlement.settle(fareShare, fee, heldAmount - charge, uncovered, now, debtDueAt);

            if (uncovered > 0) {
                notificationSender.send(memberId, potId, NotificationType.PAYMENT, "미수금 발생",
                        "정산 후 미수금 %,d원이 남았습니다. 3일 안에 납부해 주세요.".formatted(uncovered));
            }
            // 6. 신고 이력이 있는 탑승자만 '신고 없이 탑승 완료' 연속 횟수 +1 (3회면 신고 누적 −1)
            if (user.getReportCount() >= 1) {
                user.increaseCleanStreak();
            }
        }

        // 1·5. 청구 승인 + 방 종료(COMPLETED)
        pot.approveFareClaim(actualFare, now);

        // 7. 정산 완료·평가 요청 알림
        List<Long> memberIds = List.copyOf(boarded.keySet());
        notificationSender.sendAll(memberIds, potId, NotificationType.PAYMENT, "정산 완료",
                "'%s' 정산이 끝났습니다. 1인 분담금 %,d원 + 수수료 %,d원".formatted(pot.getTitle(), fareShare, fee));
        notificationSender.sendAll(memberIds, potId, NotificationType.RATING_REQUEST, "동승자 평가",
                "함께 탄 동승자를 평가해 주세요.");
    }
}
