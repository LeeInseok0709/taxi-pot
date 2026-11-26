package com.taxipot.domain.verification.mail;

import com.taxipot.global.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * app.mail.enabled=true → SMTP 발송 (spring.mail.* 설정 필요), false → 서버 로그에 코드 출력 (로컬 개발용).
 */
@Slf4j
@Configuration
public class EmailCodeSenderConfig {

    @Bean
    public EmailCodeSender emailCodeSender(AppProperties props, ObjectProvider<JavaMailSender> mailSender) {
        if (!props.mail().enabled()) {
            return (email, code, ttl) -> log.info("[인증코드-로그 발송] {} → {} ({}분 유효)", email, code, ttl);
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("app.mail.enabled=true 인데 spring.mail.host 설정이 없습니다");
        }
        return (email, code, ttl) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.mail().from());
            message.setTo(email);
            message.setSubject("[택시팟] 이메일 인증코드");
            message.setText("인증코드: " + code + "\n" + ttl + "분 안에 입력해 주세요.");
            sender.send(message);
        };
    }
}
