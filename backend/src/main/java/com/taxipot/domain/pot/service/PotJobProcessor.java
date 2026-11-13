package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 담당: 택시팟(스케줄러) — 스케줄러 1·2·3·11 의 '방 1개 처리'. 메서드 하나 = 트랜잭션 하나 = 방 하나.
 * <p>
 * <b>왜 PotJobService 와 클래스를 나눴나</b>
 * <br>Spring 의 @Transactional 은 '다른 빈을 거쳐 호출될 때'만 적용된다. PotJobService 안에서 this.process(potId) 로 부르면
 * 트랜잭션이 아예 안 걸린다 (CONVENTIONS 8장). 그래서 '방 목록 돌기'(PotJobService)와 '방 하나 처리'(여기)를 다른 빈으로 분리했다.
 * <p>
 * <b>모든 메서드의 공통 모양</b> — 은행 창구에 비유하면 "번호표 뽑기 → 창구 앞에서 서류 다시 확인 → 처리"
 * <ol>
 *   <li>잠글 회원 id 읽기 (락 없음): 이 방의 JOINED 참여자</li>
 *   <li>lockAll(회원 id 오름차순) → USERS·WALLET 잠금, 그다음 TAXIPOT 잠금 (명세 락 순서, 데드락 방지)</li>
 *   <li><b>락을 잡은 뒤 조건을 다시 확인</b>: 목록을 읽은 뒤 방장이 탑승 시작을 눌렀거나 API 가 먼저 취소했을 수 있다
 *       → 조건이 안 맞으면 아무것도 안 하고 끝 (이중 환불·이중 노쇼 방지의 핵심)</li>
 *   <li>참여자를 다시 읽어서, 1단계에서 잠그지 못한 회원이 끼어들었으면(그 사이 입장 요청 수락 등) 이번엔 건너뛴다.
 *       TAXIPOT 을 잡은 상태에서 USERS 를 추가로 잠그면 락 순서가 뒤집혀 데드락이 날 수 있기 때문.
 *       트랜잭션이 아무것도 바꾸지 않고 끝나므로 1분 뒤 다음 실행이 새 목록으로 처리한다.</li>
 *   <li>처리 → 알림 → potUpdated(커밋 후 방송)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PotJobProcessor {

    private static final List<PotStatus> BEFORE_START = List.of(PotStatus.RECRUITING, PotStatus.CLOSED);
    private static final List<PotMemberStatus> JOINED = List.of(PotMemberStatus.JOINED);

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final PotExitHandler potExitHandler;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final TimeProvider timeProvider;
    private final AppProperties props;

    // ===================================================================== 1. 미도착 자동 퇴장

    /**
     * 스케줄러 1. 집결 기한이 지난 방에서 도착 안 한 사람을 처리한다.
     * <ul>
     *   <li><b>방장이 도착함</b>: 미도착 동승자만 각각 노쇼 퇴장 (NO_SHOW·노쇼 +1·15분 정지·전액 환불·인원 −1·KICKED 알림)</li>
     *   <li><b>방장이 미도착</b>: 방을 굴릴 사람이 없으므로 방 자체를 해산 (CANCELED). 방장만 노쇼 처리, 나머지는 패널티 없이 전원 환불,
     *       입장 요청 대기자 CANCELED, 참여자·요청자에게 POT 알림</li>
     * </ul>
     * 방장 미도착인 경우 미도착 동승자도 노쇼 처리하지 않는다: 명세가 '방장도 같은 노쇼 처리'만 적었고,
     * 방이 해산돼 '남아서 기다린 사람'과 '안 온 사람'을 구분할 의미가 없어지기 때문. (팀 확인 필요하면 여기만 바꾸면 됨)
     */
    @Transactional
    public void processNoShow(Long potId) {
        LocalDateTime now = timeProvider.now();

        Optional<Locked> lockedOpt = lockPotWithMembers(potId);
        if (lockedOpt.isEmpty()) {
            return;
        }
        Locked l = lockedOpt.get();
        Taxipot pot = l.pot();

        // 락 후 재확인: 아직 탑승 전이고 집결 기한이 지났는가
        if (!BEFORE_START.contains(pot.getStatus()) || now.isBefore(pot.getArrivalDeadline())) {
            return;
        }

        // 방장 혼자 남은 방은 여기서 건드리지 않는다 → 출발 시각에 스케줄러 2가 '패널티 없이' 해산한다.
        //   명세 2번: "출발 시각 전까지는 입장 요청 수락으로 인원을 채울 수 있음" — 집결 기한(출발 5분 전)에 1번이 먼저 돌면
        //   혼자 기다리던 방장이 노쇼 정지를 받고 방이 사라져, 2번의 '정지 없음' 규칙과 충돌한다.
        //   (팀 확인 필요 항목으로 정리해 둠)
        if (l.joined().size() < 2) {
            return;
        }

        TaxipotMember host = l.joined().stream().filter(TaxipotMember::isHost).findFirst().orElse(null);

        if (host != null && !host.hasArrived()) {
            // ---------------- 방장 미도착 → 자동 해산
            host.markNoShow(now);
            potExitHandler.penalizeNoShow(l.members().get(host.getMemberId()).user(), now);
            List<Long> requesters = potExitHandler.dissolve(pot, l.joined(), l.members(), now);

            List<Long> guests = l.joined().stream()
                    .filter(m -> !m.isHost()).map(TaxipotMember::getMemberId).toList();
            notificationSender.send(host.getMemberId(), potId, NotificationType.POT,
                    "택시팟이 해산됐어요",
                    String.format("'%s' 택시팟 집결 기한까지 방장 도착 확인이 없어 방이 해산됐어요. "
                                    + "예치금은 전액 환불됐고, %d분 동안 이용이 제한돼요.",
                            pot.getTitle(), props.pot().noShowSuspendMinutes()));
            notificationSender.sendAll(concat(guests, requesters), potId, NotificationType.POT,
                    "택시팟이 해산됐어요",
                    String.format("'%s' 택시팟 방장이 집결 기한까지 도착하지 않아 방이 해산됐어요. 예치금은 전액 환불됐어요.",
                            pot.getTitle()));
            log.info("[스케줄러 1] 방장 미도착 해산 potId={}, 환불 {}명, 요청 취소 {}명", potId, l.joined().size(), requesters.size());
        } else {
            // ---------------- 방장은 도착 → 미도착 동승자만 노쇼 퇴장
            int count = 0;
            for (TaxipotMember member : l.joined()) {
                if (member.isHost() || member.hasArrived()) {
                    continue;
                }
                potExitHandler.noShowGuest(pot, member, l.members().get(member.getMemberId()), now);
                count++;
            }
            if (count == 0) {
                return;   // 그 사이 다들 도착함 → 변경 없음, 방송도 안 함
            }
            log.info("[스케줄러 1] 미도착 퇴장 potId={}, {}명", potId, count);
        }

        potRealtimePublisher.potUpdated(potId);
    }

    // ===================================================================== 2. 방장 단독 방 자동 해산

    /**
     * 스케줄러 2. 출발 시각이 됐는데 방장 혼자인 RECRUITING 방 → 해산, 방장 환불(정지 없음), 요청자 CANCELED, POT 알림.
     * <p>출발 시각 전까지는 입장 요청 수락으로 인원을 채울 기회가 있어서 '출발 시각'에 정리한다 (명세 3.4.0).
     */
    @Transactional
    public void dissolveSolo(Long potId) {
        LocalDateTime now = timeProvider.now();

        Optional<Locked> lockedOpt = lockPotWithMembers(potId);
        if (lockedOpt.isEmpty()) {
            return;
        }
        Locked l = lockedOpt.get();
        Taxipot pot = l.pot();

        // 락 후 재확인 (그 사이 입장 요청이 수락돼 2명이 됐으면 대상 아님)
        if (pot.getStatus() != PotStatus.RECRUITING || pot.getCurrentMembers() != 1
                || pot.getDepartureTime().isAfter(now)) {
            return;
        }

        List<Long> requesters = potExitHandler.dissolve(pot, l.joined(), l.members(), now);

        notificationSender.send(pot.getHostId(), potId, NotificationType.POT,
                "택시팟이 자동 해산됐어요",
                String.format("'%s' 택시팟이 출발 시각까지 함께 탈 사람이 없어 자동 해산됐어요. 예치금은 전액 환불됐어요.",
                        pot.getTitle()));
        notificationSender.sendAll(requesters, potId, NotificationType.POT,
                "입장 요청이 취소됐어요",
                String.format("'%s' 택시팟이 해산돼 입장 요청이 취소됐어요.", pot.getTitle()));

        log.info("[스케줄러 2] 방장 단독 해산 potId={}", potId);
        potRealtimePublisher.potUpdated(potId);
    }

    // ===================================================================== 3. 미출발 방 자동 취소

    /**
     * 스케줄러 3. 출발 30분이 지나도 탑승 시작을 안 누른 방 → CANCELED, 전원 환불(정지 없음), 요청자 CANCELED, POT 알림.
     * <p>방장이 버튼을 잊는 경우를 줄이려고 스케줄러 11(출발 확인 알림)이 먼저 한 번 알린다.
     */
    @Transactional
    public void cancelNotStarted(Long potId) {
        LocalDateTime now = timeProvider.now();

        Optional<Locked> lockedOpt = lockPotWithMembers(potId);
        if (lockedOpt.isEmpty()) {
            return;
        }
        Locked l = lockedOpt.get();
        Taxipot pot = l.pot();

        // 락 후 재확인: 그 사이 방장이 탑승 시작(RUNNING)했으면 대상 아님
        LocalDateTime autoCancelAt = pot.getDepartureTime().plusMinutes(props.pot().autoCancelAfterMinutes());
        if (!BEFORE_START.contains(pot.getStatus()) || autoCancelAt.isAfter(now)) {
            return;
        }

        List<Long> requesters = potExitHandler.dissolve(pot, l.joined(), l.members(), now);

        List<Long> joinedIds = l.joined().stream().map(TaxipotMember::getMemberId).toList();
        notificationSender.sendAll(concat(joinedIds, requesters), potId, NotificationType.POT,
                "택시팟이 자동 취소됐어요",
                String.format("'%s' 택시팟이 출발 %d분이 지나도록 탑승 시작이 없어 자동 취소됐어요. 예치금은 전액 환불됐어요.",
                        pot.getTitle(), props.pot().autoCancelAfterMinutes()));

        log.info("[스케줄러 3] 미출발 자동 취소 potId={}, 환불 {}명", potId, joinedIds.size());
        potRealtimePublisher.potUpdated(potId);
    }

    // ===================================================================== 11. 출발 확인 알림

    /**
     * 스케줄러 11. 전원 도착 3분 후에도 탑승 시작이 없으면 방장에게 1회 알림, start_reminded_at = now.
     * <p>지갑을 안 바꾸므로 회원 락 없이 TAXIPOT 만 잠근다. TAXIPOT 은 락 순서의 '마지막'이라 이것만 잡는 건 데드락을 만들지 않는다.
     * 락은 '두 스케줄러 실행이 겹쳐도 알림이 두 번 안 나가게' 하는 용도.
     */
    @Transactional
    public void remindStart(Long potId) {
        LocalDateTime now = timeProvider.now();
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElse(null);
        if (pot == null || !BEFORE_START.contains(pot.getStatus()) || pot.getStartRemindedAt() != null) {
            return;
        }

        // 조회 쿼리 조건을 락 후 다시 확인 (그 사이 누가 나가거나 새로 수락됐을 수 있음)
        List<TaxipotMember> joined = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, JOINED);
        boolean allArrived = joined.stream().allMatch(TaxipotMember::hasArrived);
        if (joined.size() < 2 || !allArrived) {
            return;
        }
        LocalDateTime lastArrival = joined.stream().map(TaxipotMember::getArrivedAt)
                .max(LocalDateTime::compareTo).orElseThrow();
        if (lastArrival.plusMinutes(props.pot().startReminderMinutes()).isAfter(now)) {
            return;
        }

        notificationSender.send(pot.getHostId(), potId, NotificationType.POT,
                "모두 모였어요",
                "모두 모였어요. 출발했다면 탑승 시작 버튼을 눌러주세요.");   // 명세 문구
        pot.markStartReminded(now);   // 1회만: 이후 인원이 바뀌어도 재발송 안 함
        log.info("[스케줄러 11] 출발 확인 알림 potId={}", potId);
    }

    // ===================================================================== 공통

    /** 락을 잡은 방 + JOINED 참여자 + 잠근 회원·지갑 */
    private record Locked(Taxipot pot, List<TaxipotMember> joined, Map<Long, LockedMember> members) {
    }

    /**
     * USERS·WALLET(JOINED 참여자 전원, id 오름차순) → TAXIPOT 순으로 잠그고, 참여자를 다시 읽는다.
     * 방이 없거나(삭제) 잠그지 못한 참여자가 생겼으면 empty → 이번 실행은 건너뛴다.
     */
    private Optional<Locked> lockPotWithMembers(Long potId) {
        List<Long> memberIds = taxipotMemberRepository.findMemberIds(potId, JOINED);           // 1. 락 없이 읽기
        Map<Long, LockedMember> members = memberIds.isEmpty()
                ? Map.of()
                : memberWalletLocker.lockAll(memberIds);                                       // 2. USERS → WALLET
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElse(null);               // 3. TAXIPOT
        if (pot == null) {
            return Optional.empty();
        }

        List<TaxipotMember> joined = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, JOINED);
        boolean allLocked = joined.stream().allMatch(m -> members.containsKey(m.getMemberId()));
        if (!allLocked) {
            // 1단계와 3단계 사이에 입장 요청 수락 등으로 참여자가 늘었다. 지금 그 회원을 잠그면 락 순서가 뒤집히므로 다음 실행에 맡긴다.
            log.info("[스케줄러] potId={} 처리 중 참여자 변경 → 다음 실행에서 재시도", potId);
            return Optional.empty();
        }
        return Optional.of(new Locked(pot, joined, members));
    }

    private static List<Long> concat(List<Long> a, List<Long> b) {
        List<Long> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }
}
