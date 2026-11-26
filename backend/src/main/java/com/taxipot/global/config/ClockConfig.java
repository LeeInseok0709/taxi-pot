package com.taxipot.global.config;

import com.taxipot.global.time.Kst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(Kst.ZONE);
    }
}
