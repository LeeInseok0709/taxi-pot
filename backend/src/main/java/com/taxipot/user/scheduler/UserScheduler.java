package com.taxipot.user.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 명세 '서버 배치(스케줄러)' 작업. 담당: 기성. 메서드 이름·주기 변경 금지.
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
public class UserScheduler {

    /** [5번] 정지 만료 해제 (1분): SUSPENDED이고 suspended_until <= now → ACTIVE (NULL = 영구는 제외) */
    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredSuspensions() {
        // TODO(기성)
    }

    /** [6번] 탈퇴 회원 개인정보 파기 (매일 04:00): withdrawn_at + 14일 <= now → 개인정보·계좌·WITHDRAWAL 스냅샷 NULL, 닉네임 탈퇴회원_{id}, STUDENT_VERIFICATION 삭제 + 캡처 파일 삭제 */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purgeWithdrawnUsers() {
        // TODO(기성)
    }
}
