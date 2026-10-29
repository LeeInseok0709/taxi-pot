package com.taxipot.global.error;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 명세 components.schemas.ErrorResponse. code·message·detail·timestamp 4개 키는 항상 내려간다 (detail 은 null 가능).
 * <p>@JsonInclude(NON_NULL) 을 붙이지 않는다 — 명세상 detail 은 required + nullable.
 */
public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> detail,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode code, String message, Map<String, Object> detail, OffsetDateTime now) {
        return new ErrorResponse(code.name(), message == null ? code.getDefaultMessage() : message, detail, now);
    }
}
