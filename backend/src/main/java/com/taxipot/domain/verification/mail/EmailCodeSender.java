package com.taxipot.domain.verification.mail;

/** 인증코드 메일 발송. app.mail.enabled=false 면 로그 출력 구현 사용 */
public interface EmailCodeSender {

    void send(String email, String code, long ttlMinutes);
}
