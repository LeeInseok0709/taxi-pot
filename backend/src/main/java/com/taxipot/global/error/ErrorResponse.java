package com.taxipot.global.error;

import com.taxipot.global.util.TimeUtil;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 명세 ErrorResponse: code, message, detail, timestamp 네 키가 '항상' 있어야 한다 (required).
 * detail이 없으면 키를 빼지 않고 "detail": null로 내려간다 → @JsonInclude(NON_NULL) 쓰지 말 것.
 */
public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> detail,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> detail) {
        return new ErrorResponse(errorCode.getCode(), message, detail, TimeUtil.nowOffset());
    }
}
