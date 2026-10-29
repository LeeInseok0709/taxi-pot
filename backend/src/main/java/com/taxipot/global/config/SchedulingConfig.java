package com.taxipot.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** app.scheduler.enabled=true 일 때만 @Scheduled 작업 실행 (구현 전 로그 폭주 방지) */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true")
public class SchedulingConfig {
}
