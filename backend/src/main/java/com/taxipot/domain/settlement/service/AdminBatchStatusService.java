package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.BatchStatusItem;
import com.taxipot.domain.settlement.dto.BatchStatusRequest;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminBatchStatusService {

    /** 방에 아직 남아 있는 참여자 상태 (탑승 결과를 반영할 대상) */
    private static final List<PotMemberStatus> ACTIVE = List.of(PotMemberStatus.JOINED, PotMemberStatus.BOARDED);
    /** members 로 지정할 수 있는 오프라인 결과 */
    private static final Set<PotMemberStatus> ALLOWED_MEMBER_RESULTS =
            Set.of(PotMemberStatus.BOARDED, PotMemberStatus.NO_SHOW, PotMemberStatus.CANCELED);

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final SettlementRepository settlementRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final WalletLedger walletLedger;
    private final SettlementExecutor settlementExecutor;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final AppProperties props;
    private final TimeProvider timeProvider;

    /**
     * <b>POST /api/admin/pots/batch-status</b> — 장애 복구 후 상태 일괄 입력 (선택 기능)
     * <p>
     * 서버 장애 동안 오프라인으로 처리된 매칭·탑승 결과를 복구 후 일괄 반영 (요구사항 0.2 신뢰도).
     * 한 요청 안의 항목은 하나의 트랜잭션으로 처리.
     * - potStatus = COMPLETED이면 actualFare 필수 (DB CHECK chk_taxipot_completed_fare) → 청구 승인과 같은 로직(방장 정산금·미수금 포함)으로 정산까지 처리,
     *   fare_claim_status = APPROVED, fare_reviewed_at = now (DB CHECK chk_taxipot_completed_claim)
     * - 24시간 내 미청구(OVERDUE)·장기 미처리 방 정리에도 사용 (3.5.0): 증빙을 확인해 COMPLETED(정산) 또는 CANCELED(전액 환불)로 반영
     * - 영수증 사진 없이 처리 가능 (receipt_img_path = NULL, 운영자가 오프라인 증빙 확인)
     * - CANCELED로 반영하면 HELD 정산은 전액 환불(REFUNDED)
     * <p>
     * 구현 메모 (명세에 없는 세부 규칙):
     * - potStatus 는 결과 상태인 COMPLETED·CANCELED 만 받는다 (그 외 400). 이미 끝난 방도 400
     * - members 는 오프라인 탑승 결과(BOARDED·NO_SHOW·CANCELED)만 받고, 지정하지 않은 참여자는 지금 상태를 그대로 쓴다
     *   (COMPLETED 일 때 JOINED 로 남은 사람은 탑승자로 본다). NO_SHOW 로 지정해도 정지 등 제재는 하지 않고 예치금만 환불한다
     * - 탑승하지 않은 사람(NO_SHOW·CANCELED)의 예치금은 전액 환불, 탑승자는 SettlementExecutor 로 정산
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT / INVALID_DATETIME_FORMAT
     * - 403: FORBIDDEN
     */
    @Transactional
    public void applyBatchStatus(Long adminId, BatchStatusRequest request) {
        List<BatchStatusItem> items = request.items();
        Set<Long> seen = new HashSet<>();
        for (BatchStatusItem item : items) {
            validate(item);
            if (!seen.add(item.potId())) {
                throw Errors.invalidInput("같은 택시팟이 두 번 들어 있습니다: " + item.potId());
            }
        }
        LocalDateTime now = timeProvider.now();
        for (BatchStatusItem item : items) {
            apply(item, now);
        }
    }

    // =====================================================================
    // 항목 하나 처리
    // =====================================================================

    private void apply(BatchStatusItem item, LocalDateTime now) {
        Long potId = item.potId();

        // 락 순서: 예치 중(HELD)인 회원 전원 USERS·WALLET(오름차순) → TAXIPOT
        List<Settlement> held = settlementRepository.findAllByPotIdAndStatus(potId, SettlementStatus.HELD);
        Map<Long, LockedMember> locked = memberWalletLocker.lockAll(held.stream().map(Settlement::getMemberId).toList());
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (pot.getStatus() == PotStatus.COMPLETED || pot.getStatus() == PotStatus.CANCELED) {
            throw Errors.invalidInput("이미 끝난 택시팟입니다: " + potId);
        }

        // 1. 오프라인 탑승 결과를 참여자 상태에 반영
        Map<Long, TaxipotMember> members = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, ACTIVE)
                .stream().collect(Collectors.toMap(TaxipotMember::getMemberId, Function.identity()));
        applyMemberResults(item, members, now);

        // 2. 탑승자 / 미탑승자 나누기 (예치 중인 사람 기준)
        Set<Long> boardedIds = members.values().stream()
                .filter(m -> item.potStatus() == PotStatus.COMPLETED && isRiding(m))
                .map(TaxipotMember::getMemberId)
                .collect(Collectors.toSet());
        Map<Long, LockedMember> boarded = new LinkedHashMap<>();
        for (Settlement s : held) {
            LockedMember lm = locked.get(s.getMemberId());
            if (boardedIds.contains(s.getMemberId())) {
                boarded.put(s.getMemberId(), lm);
            } else {
                // 3. 타지 않은 사람(또는 방 취소)은 예치금 전액 환불
                walletLedger.refundHold(lm.wallet(), potId, s.getHeldAmount());
                s.refund();
            }
        }

        // 4. 방 결과 반영
        if (item.potStatus() == PotStatus.CANCELED) {
            pot.cancel();
            notificationSender.sendAll(List.copyOf(locked.keySet()), potId, NotificationType.POT, "택시팟 취소",
                    "운영자가 '%s'을(를) 취소 처리했습니다. 예치금은 전액 환불됐어요.".formatted(pot.getTitle()));
        } else {
            for (Long memberId : boarded.keySet()) {
                TaxipotMember m = members.get(memberId);
                if (m != null && m.getStatus() != PotMemberStatus.BOARDED) {
                    m.board();   // 오프라인으로 탄 사람(JOINED) → BOARDED
                }
            }
            settlementExecutor.execute(pot, item.actualFare(), boarded, now);   // 정산 + COMPLETED + 알림
        }
        potRealtimePublisher.potUpdated(potId);
    }

    /** members 로 지정한 결과를 참여자 상태에 반영 */
    private static void applyMemberResults(BatchStatusItem item, Map<Long, TaxipotMember> members, LocalDateTime now) {
        if (item.members() == null) {
            return;
        }
        for (BatchStatusItem.Member result : item.members()) {
            TaxipotMember m = members.get(result.memberId());
            if (m == null) {
                throw Errors.invalidInput("이 택시팟에 남아 있는 참여자가 아닙니다: " + result.memberId());
            }
            switch (result.status()) {
                case BOARDED -> m.board();
                case NO_SHOW -> m.markNoShow(now);
                case CANCELED -> m.cancel(now);
                default -> throw Errors.invalidInput("members.status 는 BOARDED·NO_SHOW·CANCELED 만 가능합니다.");
            }
        }
    }

    /** 정산 대상(탑승)인지: BOARDED 또는 결과 지정 없이 JOINED 로 남은 사람 */
    private static boolean isRiding(TaxipotMember m) {
        return m.getStatus() == PotMemberStatus.BOARDED || m.getStatus() == PotMemberStatus.JOINED;
    }

    private void validate(BatchStatusItem item) {
        if (item.potStatus() != PotStatus.COMPLETED && item.potStatus() != PotStatus.CANCELED) {
            throw Errors.invalidInput("potStatus 는 COMPLETED 또는 CANCELED 만 가능합니다.");
        }
        if (item.potStatus() == PotStatus.COMPLETED) {
            Integer fare = item.actualFare();
            if (fare == null || fare < 0 || fare > props.pot().maxFare()) {
                throw Errors.invalidInput("COMPLETED 는 actualFare(0 ~ %,d원)가 필요합니다.".formatted(props.pot().maxFare()));
            }
        }
        if (item.members() != null) {
            for (BatchStatusItem.Member m : item.members()) {
                if (m.memberId() == null || m.status() == null || !ALLOWED_MEMBER_RESULTS.contains(m.status())) {
                    throw Errors.invalidInput("members 는 memberId 와 status(BOARDED·NO_SHOW·CANCELED)가 필요합니다.");
                }
            }
        }
    }
}
