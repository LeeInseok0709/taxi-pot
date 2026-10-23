package com.taxipot.global.error;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErrorResponse.detail 만들기용. Map.of()를 쓰면 안 되는 이유:
 * SUSPENDED_USER.suspendedUntil(영구 정지), NOT_VERIFIED.reviewStatus, UNPAID_DEBT_EXISTS.debtDueAt은
 * 명세상 null일 수 있는데, Map.of()는 null 값에서 NullPointerException을 던진다.
 *
 * 사용: ErrorDetail.of("requiredAmount", 7000, "balance", 2500, "shortageAmount", 4500)
 * 일시 값은 LocalDateTime을 넣어도 자동으로 +09:00 OffsetDateTime으로 바꿔 준다.
 */
public final class ErrorDetail {

    private ErrorDetail() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("key, value 쌍으로 넣어야 합니다.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value instanceof LocalDateTime ldt) {
                value = com.taxipot.global.util.TimeUtil.toOffset(ldt);
            } else if (value instanceof Enum<?> e) {
                value = e.name();
            }
            map.put((String) keyValues[i], value);
        }
        return Collections.unmodifiableMap(map);
    }
}
