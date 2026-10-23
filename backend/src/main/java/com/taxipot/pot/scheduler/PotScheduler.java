package com.taxipot.pot.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 명세 '서버 배치(스케줄러)' 작업. 담당: 인석. 메서드 이름·주기 변경 금지.
 * 규칙
 * - 이 클래스에는 @Transactional을 붙이지 않는다. 대상 id 목록만 조회하고,
 *   방(또는 회원) 하나씩 '다른 Bean'의 @Transactional 메서드를 호출한다 (같은 클래스 안 호출은 트랜잭션 무시됨)
 * - 처리 Bean에서는 락 순서(USERS → WALLET → TAXIPOT, id 오름차순)를 지키고, 락을 잡은 뒤 상태를 다시 확인한다
 *   (API와 동시에 돌아도 이중 환불 없음)
 * - 한 건이 실패해도 나머지는 계속: 건별로 try-catch 후 log.error
 * - 현재 시각은 TimeUtil.now(), 기준 시간은 AppProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PotScheduler {

    /** [1번] 미도착 자동 퇴장 (1분): RECRUITING·CLOSED이고 arrival_deadline <= now인 방. arrived_at NULL인 JOINED 동승자 → NO_SHOW(userManager.recordNoShow, walletManager.refundHold), 방장 미도착이면 방 CANCELED + 전원 환불 + 방장 노쇼, 남은 REQUESTED → CANCELED */
    @Scheduled(fixedDelay = 60_000)
    public void exitNoShowMembers() {
        // TODO(인석)
    }

    /** [2번] 방장 단독 방 자동 해산 (1분): departure_time <= now, RECRUITING, current_members = 1 → CANCELED, 방장 환불(정지 없음), REQUESTED → CANCELED */
    @Scheduled(fixedDelay = 60_000)
    public void dissolveHostOnlyPots() {
        // TODO(인석)
    }

    /** [3번] 미출발 방 자동 취소 (1분): departure_time + 30분 <= now, RECRUITING·CLOSED → CANCELED, 전원 환불(정지 없음) */
    @Scheduled(fixedDelay = 60_000)
    public void cancelUnstartedPots() {
        // TODO(인석)
    }

    /** [4번] 지각 경고 (1분): 집결 기한 N분 전(app.arrival.late-warning-minutes)까지 미도착 참여자에게 LATE_WARNING 알림 */
    @Scheduled(fixedDelay = 60_000)
    public void sendLateWarnings() {
        // TODO(인석)
    }

    /** [11번] 출발 확인 알림 (30초): 전원 도착 + MAX(arrived_at) + 3분 <= now, start_reminded_at NULL → 방장에게 POT 알림 1회 */
    @Scheduled(fixedDelay = 30_000)
    public void sendStartReminders() {
        // TODO(인석)
    }
}
