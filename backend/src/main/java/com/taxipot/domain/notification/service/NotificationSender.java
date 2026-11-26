package com.taxipot.domain.notification.service;

import com.taxipot.domain.notification.entity.Notification;
import com.taxipot.domain.notification.push.PushSender;
import com.taxipot.domain.notification.repository.NotificationRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

/**
 * 알림 발송 공용 진입점 (모든 도메인이 사용). NOTIFICATION 행은 호출한 트랜잭션에 함께 저장되고,
 * FCM 푸시는 커밋된 뒤에만 보낸다 (롤백된 일의 알림이 가지 않게).
 * <pre>
 * notificationSender.send(memberId, potId, NotificationType.POT, "택시팟 마감", "모집 인원이 모두 찼어요");
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PushSender pushSender;

    @Transactional
    public void send(Long memberId, Long potId, NotificationType type, String title, String content) {
        notificationRepository.save(Notification.of(memberId, potId, type, title, content));
        String token = userRepository.findById(memberId).map(User::getFcmToken).orElse(null);
        if (token != null) {
            afterCommit(() -> pushSender.send(token, title, content, potId));
        }
    }

    @Transactional
    public void sendAll(Collection<Long> memberIds, Long potId, NotificationType type, String title, String content) {
        memberIds.forEach(id -> send(id, potId, type, title, content));
    }

    /** 운영자 전원 (스케줄러 9번 '정산 청구 없음' 등) */
    @Transactional
    public void sendToAdmins(Long potId, NotificationType type, String title, String content) {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN && !u.isWithdrawn())
                .forEach(u -> send(u.getId(), potId, type, title, content));
    }

    private static void afterCommit(Runnable task) {
        Runnable safe = () -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.warn("푸시 발송 실패", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }
}
