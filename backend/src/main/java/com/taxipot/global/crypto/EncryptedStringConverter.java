package com.taxipot.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Entity 필드 자동 암·복호화. 사용: {@code @Convert(converter = EncryptedStringConverter.class)}
 * <p>Spring Boot 가 Hibernate 에 Spring 빈 컨테이너를 연결하므로 생성자 주입이 동작한다.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesGcmCipher cipher;

    public EncryptedStringConverter(AesGcmCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
