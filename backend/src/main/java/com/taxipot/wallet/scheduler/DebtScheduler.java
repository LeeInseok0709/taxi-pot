package com.taxipot.wallet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 명세 '서버 배치(스케줄러)' 작업. 담당: 효우. 메서드 이름·주기 변경 금지.
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
public class DebtScheduler {

    /** [8번] 미수금 연체 표시 (10분): UNPAID이고 debt_due_at <= now → OVERDUE + PAYMENT 알림 (금액 변경 없음) */
    @Scheduled(fixedDelay = 600_000)
    public void markOverdueDebts() {
        // TODO(효우)
    }
}
