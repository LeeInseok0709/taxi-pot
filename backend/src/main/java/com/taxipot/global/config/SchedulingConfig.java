package com.taxipot.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 11개(명세 '서버 배치')를 켠다. app.scheduler.enabled=false면 전부 꺼짐
 * (API만 테스트할 때, 같은 DB를 여러 명이 쓸 때 서로의 방을 자동 퇴장시키는 사고 방지)
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class SchedulingConfig {
}
