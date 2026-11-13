package com.taxipot.domain.chat.service;

import com.taxipot.domain.notification.push.PushSender;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * 담당: 택시팟
 * <p>채팅 새 메시지 푸시 (명세 3.12.0). 앱을 내려둬서 STOMP 가 끊긴 참여자에게 알린다.
 * <ul>
 *   <li>알림 목록(NOTIFICATION)에는 저장하지 않는다 — 채팅이 알림 목록을 덮지 않게 푸시만 보낸다. 그래서 NotificationSender 를 쓰지 않는다.</li>
 *   <li>내용 미리보기 없이 '새 메시지가 있어요' 만 보낸다. 제목은 방 제목.</li>
 *   <li>메시지마다 보낸다 (묶지 않음).</li>
 *   <li>DB 저장이 커밋된 뒤에 보낸다. 푸시 실패는 메시지 전송을 실패시키지 않는다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushNotifier {

    public static final String NEW_MESSAGE_CONTENT = "새 메시지가 있어요";

    private final UserRepository userRepository;
    private final PushSender pushSender;

    /** receiverIds = 보낸 사람을 뺀 참여자. 푸시 토큰이 없는 회원은 건너뛴다. */
    public void notifyNewMessage(Collection<Long> receiverIds, Long potId, String potTitle) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        // 토큰은 트랜잭션 안에서 미리 읽고, 발송만 커밋 뒤로 미룬다
        List<String> tokens = userRepository.findAllById(receiverIds).stream()
                .map(User::getFcmToken)
                .filter(token -> token != null && !token.isBlank())
                .toList();
        if (tokens.isEmpty()) {
            return;
        }
        afterCommit(() -> tokens.forEach(token -> sendQuietly(token, potId, potTitle)));
    }

    private void sendQuietly(String token, Long potId, String potTitle) {
        try {
            pushSender.send(token, potTitle, NEW_MESSAGE_CONTENT, potId);
        } catch (RuntimeException e) {
            log.warn("채팅 푸시 발송 실패 potId={}", potId, e);
        }
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
