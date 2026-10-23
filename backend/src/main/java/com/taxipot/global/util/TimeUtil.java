package com.taxipot.global.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * 모든 시각은 이 클래스로만 만든다. (LocalDateTime.now()를 직접 쓰지 않는다)
 * - Entity: LocalDateTime(KST)  /  DTO·ErrorResponse.detail: OffsetDateTime(+09:00)
 * - 초 단위로 자른다: MySQL DATETIME은 소수초를 '반올림'해서 저장하므로, 자르지 않으면
 *   저장값이 1초 뒤로 밀려 "집결 기한 <= now" 같은 비교가 어긋날 수 있다.
 */
public final class TimeUtil {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private TimeUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
    }

    public static OffsetDateTime nowOffset() {
        return now().atOffset(KST_OFFSET);
    }

    /** Entity → DTO. null이면 null (nullable 필드에 그대로 사용). */
    public static OffsetDateTime toOffset(LocalDateTime kst) {
        return kst == null ? null : kst.truncatedTo(ChronoUnit.SECONDS).atOffset(KST_OFFSET);
    }

    /** Request DTO → Entity. 어떤 오프셋으로 와도 KST로 바꾼다. */
    public static LocalDateTime toKst(OffsetDateTime value) {
        return value == null ? null
                : value.atZoneSameInstant(KST).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
