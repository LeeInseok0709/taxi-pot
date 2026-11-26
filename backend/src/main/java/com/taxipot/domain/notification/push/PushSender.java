package com.taxipot.domain.notification.push;

/**
 * FCM 푸시 발송. 구현은 {@link PushSenderConfig}가 app.push.enabled 값으로 하나만 고른다
 * (false → LogPushSender 로그 출력, true → FcmPushSender 실제 발송).
 */
public interface PushSender {

    void send(String fcmToken, PushMessage message);
}
