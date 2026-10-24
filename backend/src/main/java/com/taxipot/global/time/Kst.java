package com.taxipot.global.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * KST 변환 도우미. Entity 는 LocalDateTime(KST), DTO 는 OffsetDateTime(+09:00).
 * <p>Entity → DTO: {@code Kst.toOffset(entity.getCreatedAt())} (null 이면 null)
 */
public final class Kst {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);

    private Kst() {
    }

    public static OffsetDateTime toOffset(LocalDateTime kst) {
        return kst == null ? null : kst.atOffset(OFFSET);
    }

    /** 요청으로 받은 일시를 KST LocalDateTime 으로 (다른 오프셋이면 KST 로 환산, 초 미만 버림) */
    public static LocalDateTime toLocal(OffsetDateTime time) {
        return time == null ? null
                : time.atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
    }

    /** Entity @PrePersist 용 현재 시각 (DB DATETIME 과 맞추려고 초 단위로 자름) */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }
}
