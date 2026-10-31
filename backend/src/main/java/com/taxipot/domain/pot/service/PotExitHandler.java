package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 담당: 택시팟(도착·스케줄러) — '방에서 나가게 하는 처리'를 한곳에 모은 컴포넌트.
 * <p>
 * 왜 따로 뺐나: 같은 '퇴장 + 환불'을 여러 곳이 쓴다.
 * <ul>
 *   <li>방장 내보내기 API (집결 기한 전 KICKED / 기한 후 NO_SHOW)</li>
 *   <li>스케줄러 1 미도착 자동 퇴장 (NO_SHOW), 방장 미도착 시 자동 해산</li>
 *   <li>스케줄러 2·3 방 해산·자동 취소</li>
 *   <li>(같은 패키지의 참여 취소·방장 방 취소 API 도 이걸 쓰면 환불 규칙이 한 벌로 유지된다)</li>
 * </ul>
 * 비유하면 '퇴실 정산 창구'다. 어느 경로로 나가든 같은 창구를 거치니, 환불 규칙(원장 기록·정산 행 REFUNDED)을 한 번만 맞추면 된다.
 * <p>
 * <b>전제</b>: 모든 메서드는 호출 측 트랜잭션 안에서, 대상 회원의 USERS → WALLET 과 TAXIPOT 락을 이미 잡은 상태로 부른다
 * (Propagation.MANDATORY 로 트랜잭션 없이 부르면 바로 예외가 나게 해 두었다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PotExitHandler {

    private final SettlementRepository settlementRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final WalletLedger walletLedger;
    private final NotificationSender notificationSender;
    private final AppProperties props;

    // ===================================================================== 환불

    /**
     * 이 방에 묶인 예치금을 전액 돌려준다 (SETTLEMENT → REFUNDED, POINT_HISTORY FARE_REFUND +예치액).
     * <p>
     * <b>멱등</b>: 정산 행이 HELD 일 때만 환불한다. 이미 REFUNDED·SETTLED 이거나 행이 없으면 아무것도 하지 않는다.
     * 이 검사는 '락을 잡은 뒤' 실행되므로, API(참여 취소)와 스케줄러가 같은 사람을 동시에 처리해도
     * 두 번째 쪽은 첫 번째 커밋 결과(REFUNDED)를 보고 건너뛴다 → 이중 환불 없음.
     *
     * @return 환불한 금액 (환불할 게 없었으면 0) — 알림 문구에 쓴다
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int refundHold(Wallet wallet, Long potId) {
        Long memberId = wallet.getMemberId();
        Settlement settlement = settlementRepository.findByPotIdAndMemberId(potId, memberId).orElse(null);
        if (settlement == null || settlement.getStatus() != SettlementStatus.HELD) {
            // JOINED 인데 HELD 정산 행이 없으면 데이터가 어긋난 것. 처리를 막지는 않고 로그로 남긴다.
            log.warn("[환불 건너뜀] potId={}, memberId={}, settlement={}", potId, memberId,
                    settlement == null ? "없음" : settlement.getStatus());
            return 0;
        }
        int amount = settlement.getHeldAmount();
        walletLedger.refundHold(wallet, potId, amount);   // 지갑 held → balance + 원장 FARE_REFUND
        settlement.refund();                              // REFUNDED, fare_share 등 NULL (명세: 노쇼·환불 행은 NULL)
        return amount;
    }

    // ===================================================================== 동승자 1명 퇴장

    /**
     * 집결 기한 '전' 방장 내보내기 → KICKED. 노쇼가 아니므로 정지·노쇼 집계 없음.
     * <p>처리: status = KICKED, canceled_at = now, 인원 − 1(has_canceled = 1, CLOSED → RECRUITING), 전액 환불, KICKED 알림
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void kickGuest(Taxipot pot, TaxipotMember member, LockedMember target, LocalDateTime now) {
        member.kick(now);
        pot.removeMember();
        int refunded = refundHold(target.wallet(), pot.getId());

        notificationSender.send(member.getMemberId(), pot.getId(), NotificationType.KICKED,
                "택시팟에서 내보내졌어요",
                String.format("방장이 '%s' 택시팟에서 회원님을 내보냈어요. 예치금 %,d원은 전액 환불됐어요.",
                        pot.getTitle(), refunded));
    }

    /**
     * 노쇼 퇴장 (동승자). 스케줄러 1 자동 퇴장, 집결 기한 '후' 방장 내보내기 둘 다 여기로 온다.
     * <p>처리: status = NO_SHOW, canceled_at = now, no_show_count + 1, 15분 정지(더 긴 쪽 유지),
     * 인원 − 1(has_canceled = 1, CLOSED → RECRUITING), 전액 환불(금전 페널티 없음), KICKED 알림
     * <p>
     * 명세 '노쇼 집계 규칙': 노쇼 집계는 이 시점에서만 1회. 그래서 노쇼 판정 + 집계 + 정지를 한 메서드에 묶어 따로 부를 수 없게 했다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void noShowGuest(Taxipot pot, TaxipotMember member, LockedMember target, LocalDateTime now) {
        member.markNoShow(now);
        penalizeNoShow(target.user(), now);
        pot.removeMember();
        int refunded = refundHold(target.wallet(), pot.getId());

        notificationSender.send(member.getMemberId(), pot.getId(), NotificationType.KICKED,
                "집결 기한 미도착으로 퇴장됐어요",
                String.format("'%s' 택시팟 집결 기한까지 도착 확인이 없어 퇴장 처리됐어요. "
                                + "예치금 %,d원은 전액 환불됐고, %d분 동안 이용이 제한돼요.",
                        pot.getTitle(), refunded, props.pot().noShowSuspendMinutes()));
    }

    /** 노쇼 페널티: no_show_count + 1, now + 15분 정지 (User.suspend 가 '기존 정지와 더 긴 쪽 유지' 규칙을 처리) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void penalizeNoShow(User user, LocalDateTime now) {
        user.increaseNoShowCount();
        user.suspend(now.plusMinutes(props.pot().noShowSuspendMinutes()));
    }

    // ===================================================================== 방 전체 해산

    /**
     * 방 해산 공통 처리: 남은 참여자 전원 환불 + 방 CANCELED + 입장 요청 대기자 CANCELED.
     * 스케줄러 1(방장 미도착), 2(방장 단독), 3(출발 30분 후 미출발)이 쓴다. 알림 문구는 사유마다 달라서 호출 측이 보낸다.
     * <p>
     * 참여자(JOINED) 행의 status 는 바꾸지 않는다. 명세의 방 취소(DELETE /api/pots/{potId})도 '방 CANCELED + 환불'만 적혀 있고,
     * 방이 CANCELED 이면 '진행 중인 방' 조회에서 빠지므로 다른 방에 바로 참여할 수 있다.
     * 인원(current_members)과 has_canceled 도 바꾸지 않는다 (명세: 방 전체 취소는 status = CANCELED 로만 표현).
     *
     * @param joined 이 방의 JOINED 참여자 (방장 포함). 모두 {@code locked} 에 들어 있어야 한다
     * @param locked lockAll 로 잠근 회원·지갑
     * @return CANCELED 로 바꾼 입장 요청자 member_id 목록 (알림 대상)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> dissolve(Taxipot pot, List<TaxipotMember> joined, Map<Long, LockedMember> locked,
                               LocalDateTime now) {
        for (TaxipotMember member : joined) {
            refundHold(locked.get(member.getMemberId()).wallet(), pot.getId());
        }
        pot.cancel();
        return cancelPendingRequests(pot.getId(), now);
    }

    /**
     * 입장 요청 대기자(REQUESTED) → CANCELED, canceled_at = now.
     * 입장 요청은 '수락' 시점에 예치하므로 REQUESTED 상태에는 예치금이 없다 → 지갑 락·환불이 필요 없다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> cancelPendingRequests(Long potId, LocalDateTime now) {
        List<TaxipotMember> requests = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(
                potId, List.of(PotMemberStatus.REQUESTED));
        requests.forEach(r -> r.cancel(now));
        return requests.stream().map(TaxipotMember::getMemberId).toList();
    }
}
