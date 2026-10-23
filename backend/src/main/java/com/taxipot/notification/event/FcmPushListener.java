package com.taxipot.notification.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋된 알림만 푸시 (롤백된 처리의 푸시가 나가는 사고 방지). 실패해도 본 처리에 영향 없음.
 * TODO(기성): USERS.fcm_token 조회 후 Firebase Admin SDK로 발송
 */
@Slf4j
@Component
public class FcmPushListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void push(NotificationCreatedEvent event) {
        log.info("[FCM 미구현] memberId={}, title={}", event.memberId(), event.title());
    }
}
