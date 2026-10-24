package com.taxipot.global.time;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 요청 일시 문자열 검사·변환 (명세 '일시(KST) 규칙').
 * <ul>
 *   <li>오프셋 필수: {@code 2026-10-16T23:30:00+09:00} 형식만 허용</li>
 *   <li>{@code Z}(UTC)·오프셋 없는 값 → 실패 (호출 측에서 400 INVALID_DATETIME_FORMAT)</li>
 *   <li>+09:00 이 아닌 오프셋은 KST 로 환산</li>
 * </ul>
 */
public final class KstDateTimeParser {

    private static final Pattern OFFSET_REQUIRED =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?[+-]\\d{2}:\\d{2}$");
    /** 쿼리 파라미터에서 '+'가 인코딩되지 않아 공백으로 바뀐 경우 ('...T23:00:00 09:00') */
    private static final Pattern PLUS_AS_SPACE =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?) (\\d{2}:\\d{2})$");

    private KstDateTimeParser() {
    }

    public static OffsetDateTime parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        var m = PLUS_AS_SPACE.matcher(value);
        if (m.matches()) {
            value = m.group(1) + "+" + m.group(4);
        }
        if (!OFFSET_REQUIRED.matcher(value).matches()) {
            throw new DateTimeParseException("+09:00 오프셋이 필요합니다", raw, 0);
        }
        return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withOffsetSameInstant(Kst.OFFSET);
    }
}
