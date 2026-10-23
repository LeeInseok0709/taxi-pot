package com.taxipot.notification.service;

import com.taxipot.global.code.NotificationType;
import com.taxipot.notification.entity.Notification;
import com.taxipot.notification.event.NotificationCreatedEvent;
import com.taxipot.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발송은 전원 이 메서드 하나만 쓴다. 최하위 계층 — 어떤 Manager·서비스도 주입하지 않는다.
 * notificationManager.send(memberId, potId, NotificationType.POT, "택시팟이 마감되었어요", "모든 인원이 모였습니다.");
 * - 호출한 트랜잭션 안에서 NOTIFICATION 행 저장 → 롤백되면 알림도 같이 사라짐
 * - FCM 푸시는 커밋 후 FcmPushListener가 보냄
 * - potId가 없는 알림(PAYMENT 일부, SYSTEM)은 null
 */
@Service
@RequiredArgsConstructor
public class NotificationManager {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void send(Long memberId, Long potId, NotificationType type, String title, String content) {
        Notification saved = notificationRepository.save(Notification.builder()
                .memberId(memberId)
                .potId(potId)
                .type(type)
                .title(title)
                .content(content)
                .build());
        eventPublisher.publishEvent(new NotificationCreatedEvent(saved.getId(), memberId, title, content));
    }
}
