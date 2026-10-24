package com.taxipot.domain.notification.push;

/** FCM 푸시 발송. 실제 FCM 연동 전에는 LogPushSender (로그 출력) */
public interface PushSender {

    void send(String fcmToken, String title, String content, Long potId);
}
