package com.taxipot.global.config;

import com.taxipot.global.util.TimeUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "kstDateTimeProvider")
public class JpaConfig {

    /** created_at/updated_at도 TimeUtil.now()(KST, 초 단위)로 기록 */
    @Bean
    public DateTimeProvider kstDateTimeProvider() {
        return () -> Optional.of(TimeUtil.now());
    }
}
