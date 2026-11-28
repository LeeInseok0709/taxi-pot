package com.taxipot.domain.notification.push;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 개발용 기본 구현: 실제 발송 없이 로그만 남긴다 (app.push.enabled=false).
 * 빈 등록은 {@link PushSenderConfig}가 한다 — 여기에 @Component 를 붙이지 않는다.
 */
@Slf4j
public class LogPushSender implements PushSender {

    @Override
    public void send(String fcmToken, PushMessage message) {
        log.info("[PUSH-로그] token={}… title={} content={} data={}",
                fcmToken == null ? "null" : fcmToken.substring(0, Math.min(8, fcmToken.length())),
                message.title(), message.content(), message.data());
    }
}
