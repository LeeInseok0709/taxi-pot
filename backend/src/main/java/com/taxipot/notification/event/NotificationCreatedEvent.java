package com.taxipot.notification.event;

/** NotificationManager.send가 발행. FCM 푸시는 트랜잭션 커밋 후에만 보낸다 */
public record NotificationCreatedEvent(Long notificationId, Long memberId, String title, String content) {
}
