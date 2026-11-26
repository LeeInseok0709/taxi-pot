package com.taxipot.domain.notification.push;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM 실제 발송 (app.push.enabled=true). 빈 등록은 {@link PushSenderConfig}가 한다.
 * <ul>
 *   <li>알림(notification) = 제목·내용. 앱이 꺼져 있어도 안드로이드가 알림창에 띄운다.</li>
 *   <li>데이터(data) = {@code type}·{@code potId}·{@code notificationId} (규격은 {@link PushMessage}). 앱이 푸시를 눌렀을 때 이동할 화면을 고르는 데 쓴다.</li>
 *   <li>비동기 발송: 요청 스레드를 붙잡지 않는다. 실패는 로그만 남긴다 (알림 행 저장·업무 처리는 이미 커밋됨).</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging messaging;

    @Override
    public void send(String fcmToken, PushMessage message) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        Long potId = message.potId();
        ApiFuture<String> result = messaging.sendAsync(buildMessage(fcmToken, message));
        ApiFutures.addCallback(result, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(String messageId) {
                log.debug("FCM 발송 성공 potId={} messageId={}", potId, messageId);
            }

            @Override
            public void onFailure(Throwable t) {
                // UNREGISTERED = 앱 삭제·토큰 만료. 앱이 다음 로그인 때 새 토큰을 보내면 다시 받는다
                String code = t instanceof FirebaseMessagingException fme && fme.getMessagingErrorCode() != null
                        ? fme.getMessagingErrorCode().name() : t.getClass().getSimpleName();
                log.warn("FCM 발송 실패 potId={} code={} message={}", potId, code, t.getMessage());
            }
        }, Runnable::run);
    }

    static Message buildMessage(String fcmToken, PushMessage message) {
        return Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(message.title()).setBody(message.content()).build())
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .putAllData(message.data())
                .build();
    }
}
