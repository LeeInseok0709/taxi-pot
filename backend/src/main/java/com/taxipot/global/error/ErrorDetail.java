package com.taxipot.global.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * detail Map 생성. Map.of() 는 null 값에서 NPE 가 나므로 쓰지 않는다
 * (영구 정지 suspendedUntil, 연체만 있을 때 debtDueAt 은 null 이 정상값).
 * <p>{@code ErrorDetail.of("suspendedUntil", null)} → {"suspendedUntil": null}
 */
public final class ErrorDetail {

    private ErrorDetail() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("key, value 쌍이어야 합니다");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
