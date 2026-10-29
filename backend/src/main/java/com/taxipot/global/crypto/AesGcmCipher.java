package com.taxipot.global.crypto;

import com.taxipot.global.config.AppProperties;
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
 * AES-256-GCM (휴대폰번호·계좌번호). 저장 형식: Base64(IV 12바이트 + 암호문·태그).
 * <p>같은 평문도 매번 다른 암호문이 나오므로 암호화 컬럼으로 WHERE 검색은 할 수 없다.
 */
@Component
public class AesGcmCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCipher(AppProperties props) {
        String encoded = props.crypto() == null ? null : props.crypto().aesKey();
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("app.crypto.aes-key 가 없습니다 (환경변수 TAXIPOT_CRYPTO_KEY)");
        }
        byte[] raw = Base64.getDecoder().decode(encoded);
        if (raw.length != 32) {
            throw new IllegalStateException("app.crypto.aes-key 는 Base64 로 인코딩한 32바이트여야 합니다");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            return new String(cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("복호화 실패 (키가 바뀌었거나 평문 데이터가 들어 있음)", e);
        }
    }
}
