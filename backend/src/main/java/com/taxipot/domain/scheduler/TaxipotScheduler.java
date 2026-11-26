package com.taxipot.domain.scheduler;

import com.taxipot.domain.pot.service.PotJobService;
import com.taxipot.domain.rating.service.RatingJobService;
import com.taxipot.domain.settlement.service.SettlementJobService;
import com.taxipot.domain.user.service.MemberJobService;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 스케줄러 11개 작업의 실행 시각만 정한다 (로직은 각 담당의 *JobService).
 * app.scheduler.enabled=true 일 때만 동작. 주기는 application.yml app.scheduler.* (시연 프로필에서 줄일 수 있음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true")
public class TaxipotScheduler {

    private final PotJobService potJobService;
    private final MemberJobService memberJobService;
    private final SettlementJobService settlementJobService;
    private final RatingJobService ratingJobService;

    @Scheduled(fixedDelayString = "${app.scheduler.one-minute-ms:60000}", initialDelay = 10_000)
    public void everyMinute() {
        run("1 미도착 자동 퇴장", potJobService::autoNoShow);
        run("2 방장 단독 방 해산", potJobService::dissolveSoloPots);
        run("3 미출발 방 자동 취소", potJobService::cancelNotStartedPots);
        run("4 지각 경고", potJobService::lateWarning);
        run("5 정지 만료 해제", memberJobService::releaseExpiredSuspensions);
    }

    @Scheduled(fixedDelayString = "${app.scheduler.thirty-seconds-ms:30000}", initialDelay = 15_000)
    public void everyThirtySeconds() {
        run("11 출발 확인 알림", potJobService::startReminder);
    }

    @Scheduled(fixedDelayString = "${app.scheduler.ten-minutes-ms:600000}", initialDelay = 20_000)
    public void everyTenMinutes() {
        run("8 미수금 연체", settlementJobService::markOverdueDebts);
        run("9 청구 기한 경과", settlementJobService::markOverdueFareClaims);
    }

    @Scheduled(fixedDelayString = "${app.scheduler.one-hour-ms:3600000}", initialDelay = 30_000)
    public void everyHour() {
        run("7 평가 기본값", ratingJobService::fillDefaultRatings);
    }

    @Scheduled(cron = "${app.scheduler.daily-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void daily() {
        run("6 탈퇴 회원 파기", memberJobService::purgeWithdrawnMembers);
        run("10 영수증 파기", settlementJobService::purgeOldReceipts);
    }

    /** 한 작업이 실패해도 다음 작업은 실행 */
    private void run(String name, Runnable job) {
        try {
            job.run();
        } catch (NotImplementedYetException e) {
            log.debug("[스케줄러 {}] 아직 구현 안 됨", name);
        } catch (RuntimeException e) {
            log.error("[스케줄러 {}] 실패", name, e);
        }
    }
}
