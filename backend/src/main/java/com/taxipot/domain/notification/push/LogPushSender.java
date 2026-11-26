package com.taxipot.domain.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** FCM 연동 전 기본 구현: 로그만 남긴다. FCM 을 붙이면 PushSender 를 구현한 @Component 를 추가하면 이 빈은 빠진다 */
@Slf4j
@Component
@ConditionalOnMissingBean(value = PushSender.class, ignored = LogPushSender.class)
public class LogPushSender implements PushSender {

    @Override
    public void send(String fcmToken, String title, String content, Long potId) {
        log.info("[PUSH] token={}… title={} content={} potId={}",
                fcmToken.substring(0, Math.min(8, fcmToken.length())), title, content, potId);
    }
}
