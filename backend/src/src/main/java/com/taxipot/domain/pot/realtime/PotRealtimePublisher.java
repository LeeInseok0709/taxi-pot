package com.taxipot.domain.pot.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * STOMP /topic/pots/{potId} 방송. 커밋된 뒤에 보낸다 (앱이 받자마자 다시 조회해도 바뀐 값이 보이게).
 * <ul>
 *   <li>CHAT_MESSAGE: payload = ChatMessageResponse</li>
 *   <li>POT_UPDATED: 인원·상태·도착·입장 요청 변경 → payload = {potId}. 앱은 상세를 다시 조회</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PotRealtimePublisher {

    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String POT_UPDATED = "POT_UPDATED";

    private final SimpMessagingTemplate messagingTemplate;

    public void chatMessage(Long potId, Object chatMessageResponse) {
        publish(potId, new PotEvent(CHAT_MESSAGE, potId, chatMessageResponse));
    }

    public void potUpdated(Long potId) {
        publish(potId, new PotEvent(POT_UPDATED, potId, Map.of("potId", potId)));
    }

    private void publish(Long potId, PotEvent event) {
        Runnable send = () -> messagingTemplate.convertAndSend("/topic/pots/" + potId, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    public record PotEvent(String type, Long potId, Object payload) {
    }
}
