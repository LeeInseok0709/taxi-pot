package com.taxipot.global.time;

import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;

/**
 * 쿼리 파라미터(@RequestParam OffsetDateTime) 변환기. WebMvcConfig 에서 등록.
 * <p>실패하면 MethodArgumentTypeMismatchException → GlobalExceptionHandler 가 INVALID_DATETIME_FORMAT 으로 변환
 */
public class OffsetDateTimeParamConverter implements Converter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(String source) {
        if (source.isBlank()) {
            return null;
        }
        return KstDateTimeParser.parse(source);
    }
}
