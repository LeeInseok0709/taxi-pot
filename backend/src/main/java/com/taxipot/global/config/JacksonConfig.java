package com.taxipot.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.KstDateTimeParser;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 일시 JSON 형식 고정.
 * <ul>
 *   <li>응답: 항상 {@code yyyy-MM-dd'T'HH:mm:ss+09:00} (초 미만 없음)</li>
 *   <li>요청: 오프셋 없는 값·Z 는 400 INVALID_DATETIME_FORMAT (detail.field, detail.value)</li>
 * </ul>
 * null 값도 키를 생략하지 않는다 (명세: non_null 설정 금지) — Jackson 기본 동작 그대로 둔다.
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter RESPONSE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer kstDateTimeCustomizer() {
        return builder -> builder
                .serializerByType(OffsetDateTime.class, new KstSerializer())
                .deserializerByType(OffsetDateTime.class, new KstDeserializer());
    }

    static class KstSerializer extends JsonSerializer<OffsetDateTime> {
        @Override
        public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.withOffsetSameInstant(Kst.OFFSET).format(RESPONSE_FORMAT));
        }
    }

    static class KstDeserializer extends JsonDeserializer<OffsetDateTime> {
        @Override
        public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                throw InvalidFormatException.from(p, "일시는 문자열이어야 합니다", p.getText(), OffsetDateTime.class);
            }
            String text = p.getText();
            try {
                return KstDateTimeParser.parse(text);
            } catch (DateTimeParseException e) {
                throw InvalidFormatException.from(p, "+09:00 오프셋이 필요합니다", text, OffsetDateTime.class);
            }
        }
    }
}
