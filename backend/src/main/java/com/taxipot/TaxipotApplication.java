package com.taxipot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaxipotApplication {

    public static void main(String[] args) {
        // IntelliJ 실행 설정에 -Duser.timezone을 빼먹어도 KST로 고정 (팀원 PC마다 시간이 달라지는 문제 방지)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(TaxipotApplication.class, args);
    }
}
