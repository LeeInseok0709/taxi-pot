package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 담당: 택시팟(스케줄러) — 스케줄러 작업 1·2·3·4·11 (명세 '서버 배치(스케줄러) 작업').
 * <p>
 * 이 클래스는 '어느 방을 처리할지 고르고 하나씩 넘기는' 역할만 한다. 실제 처리는 {@link PotJobProcessor}.
 * <pre>
 *   TaxipotScheduler (언제)  →  PotJobService (어느 방)  →  PotJobProcessor (방 하나, 트랜잭션 1개)
 * </pre>
 * 방마다 트랜잭션을 따로 두는 이유:
 * <ul>
 *   <li>한 방에서 예외가 나도 그 방만 롤백되고 나머지 방은 처리된다 (forEachPot 의 try-catch)</li>
 *   <li>락을 방 하나 처리하는 짧은 시간만 잡는다. 전체를 한 트랜잭션으로 묶으면 그동안 참여·취소 API 가 줄줄이 대기한다</li>
 * </ul>
 * 대상 id 조회는 트랜잭션·락 없이 한다. 그래서 Processor 가 락을 잡은 뒤 조건을 다시 확인한다 (API 와 동시에 실행돼도 안전).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PotJobService {

    private static final List<PotStatus> BEFORE_START = List.of(PotStatus.RECRUITING, PotStatus.CLOSED);

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final PotJobProcessor potJobProcessor;
    private final NotificationSender notificationSender;
    private final TimeProvider timeProvider;
    private final AppProperties props;

    /**
     * 1. 미도착 자동 퇴장 (1분): RECRUITING·CLOSED 이고 arrival_deadline &lt;= now 인 방.
     * arrived_at 이 NULL 인 JOINED 동승자 → NO_SHOW, canceled_at = now, no_show_count + 1, 예치금 전액 환불(SETTLEMENT REFUNDED),
     * 15분 정지(User.suspend: 더 긴 쪽 유지), current_members − 1, has_canceled = 1, CLOSED 였다면 RECRUITING, KICKED 알림.
     * 방장이 미도착이면 자동 해산(CANCELED) + 전원 환불, 방장도 같은 노쇼 처리, 남은 REQUESTED → CANCELED, 참여자·요청자에게 POT 알림.
     */
    public void autoNoShow() {
        List<Long> potIds = taxipotRepository.findNoShowTargetIds(BEFORE_START, timeProvider.now());
        forEachPot("1 미도착 자동 퇴장", potIds, potJobProcessor::processNoShow);
    }

    /** 2. 방장 단독 방 자동 해산 (1분): departure_time &lt;= now, RECRUITING, current_members = 1 → CANCELED, 방장 전액 환불(정지 없음), REQUESTED → CANCELED, POT 알림 */
    public void dissolveSoloPots() {
        List<Long> potIds = taxipotRepository.findSoloPotIds(timeProvider.now());
        forEachPot("2 방장 단독 방 해산", potIds, potJobProcessor::dissolveSolo);
    }

    /** 3. 미출발 방 자동 취소 (1분): departure_time + 30분 &lt;= now 이고 RECRUITING·CLOSED → CANCELED, 전원 환불(정지 없음), REQUESTED → CANCELED, POT 알림 */
    public void cancelNotStartedPots() {
        // departure_time + 30분 <= now  ⇔  departure_time <= now − 30분
        LocalDateTime threshold = timeProvider.now().minusMinutes(props.pot().autoCancelAfterMinutes());
        List<Long> potIds = taxipotRepository.findNotStartedPotIds(BEFORE_START, threshold);
        forEachPot("3 미출발 방 자동 취소", potIds, potJobProcessor::cancelNotStarted);
    }

    /**
     * 4. 지각 경고 (1분): 집결 기한 N분(app.pot.late-warning-minutes) 전까지 미도착인 참여자에게 LATE_WARNING 알림.
     * <p>
     * '집결 기한 N분 전 ~ 집결 기한' 구간에 들어온 방의 미도착 JOINED 참여자에게 한 번만 보낸다.
     * 중복 방지는 'LATE_WARNING 알림을 이미 받았는가'로 판단한다 (TaxipotMemberRepository.findLateWarningTargets 주석 참고).
     * <p>
     * 지갑·참여 상태를 바꾸지 않으므로 락 없이 한 트랜잭션으로 처리한다. 경고 직후 그 사람이 도착 확인을 해도 문제 없다
     * (경고 문자가 이미 나간 것뿐). 알림 INSERT 와 '보냈는지 확인'이 같은 트랜잭션이라, 실행이 겹치지만 않으면 중복이 없다.
     * 스케줄러는 fixedDelay 라 이전 실행이 끝나야 다음이 시작되므로 겹치지 않는다 (서버 1대 기준).
     */
    @Transactional
    public void lateWarning() {
        LocalDateTime now = timeProvider.now();
        long minutes = props.pot().lateWarningMinutes();
        List<TaxipotMember> targets = taxipotMemberRepository.findLateWarningTargets(now, now.plusMinutes(minutes));

        for (TaxipotMember m : targets) {
            notificationSender.send(m.getMemberId(), m.getPotId(), NotificationType.LATE_WARNING,
                    "곧 집결 기한이에요",
                    String.format("집결 기한까지 %d분 이내로 남았어요. 약속 장소에 도착하면 도착 확인을 눌러주세요. "
                            + "기한까지 확인이 없으면 자동 퇴장되고 이용이 제한돼요.", minutes));
        }
        if (!targets.isEmpty()) {
            log.info("[스케줄러 4] 지각 경고 {}건", targets.size());
        }
    }

    /**
     * 11. 출발 확인 알림 (30초): RECRUITING·CLOSED, start_reminded_at IS NULL, 남은 JOINED 2명 이상 전원 arrived_at NOT NULL,
     * MAX(arrived_at) + 3분 &lt;= now → 방장에게 POT 알림, start_reminded_at = now (1회만)
     */
    public void startReminder() {
        // MAX(arrived_at) + 3분 <= now  ⇔  MAX(arrived_at) <= now − 3분
        LocalDateTime threshold = timeProvider.now().minusMinutes(props.pot().startReminderMinutes());
        List<Long> potIds = taxipotRepository.findStartReminderTargetIds(BEFORE_START, threshold);
        forEachPot("11 출발 확인 알림", potIds, potJobProcessor::remindStart);
    }

    /**
     * 방마다 Processor 를 호출하되, 한 방이 실패해도 다음 방은 계속 처리한다.
     * <p>CONVENTIONS 9장은 예외를 삼키지 말라고 하지만, 여기선 '삼키는' 게 아니라 ERROR 로그(스택 포함)를 남기고 다음 방으로 넘어가는 것.
     * 실패한 방은 트랜잭션이 롤백돼 그대로 남아 있으므로 다음 실행(1분 뒤)에 다시 대상이 된다.
     */
    private void forEachPot(String jobName, List<Long> potIds, Consumer<Long> processor) {
        for (Long potId : potIds) {
            try {
                processor.accept(potId);
            } catch (RuntimeException e) {
                log.error("[스케줄러 {}] potId={} 처리 실패 — 다음 실행에서 재시도", jobName, potId, e);
            }
        }
    }
}
