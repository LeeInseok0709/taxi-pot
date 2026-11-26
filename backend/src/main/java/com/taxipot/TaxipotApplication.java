package com.taxipot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaxipotApplication {

    public static void main(String[] args) {
        // 명세 '일시(KST) 규칙': JVM 시간대 Asia/Seoul. IntelliJ 실행 설정에 -Duser.timezone 을 빠뜨려도 동일하게 동작하도록 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(TaxipotApplication.class, args);
    }
}
