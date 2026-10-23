package com.taxipot.global.crypto;

import com.taxipot.global.config.AppProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * USERS.phone, USERS.account_no, WITHDRAWAL.account_no 암호화 (명세: AES-256-GCM, 키는 설정값).
 * Entity에는 이미 @Convert(converter = AesGcmStringConverter.class)가 붙어 있다.
 * - 같은 값도 매번 다른 암호문 → phone·account_no로 WHERE 검색·중복 검사 불가 (명세상 필요 없음)
 * - 팀 전원이 같은 키를 써야 서로의 DB 덤프를 읽을 수 있다
 * - 정규화(숫자만 남기기)는 서비스에서 저장 전에 한다
 */
@Component
@Converter
public class AesGcmStringConverter implements AttributeConverter<String, String> {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AesGcmStringConverter(AppProperties appProperties) {
        byte[] raw = Base64.getDecoder().decode(appProperties.crypto().aesKey());
        if (raw.length != 32) {
            throw new IllegalStateException("app.crypto.aes-key는 Base64로 인코딩한 32바이트 키여야 합니다.");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("복호화 실패 (aes-key가 팀원과 다른지 확인)", e);
        }
    }
}
