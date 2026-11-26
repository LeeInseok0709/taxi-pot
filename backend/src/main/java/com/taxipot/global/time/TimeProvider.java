package com.taxipot.global.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 서비스에서 '지금'은 반드시 이걸로 구한다 ({@code LocalDateTime.now()} 직접 호출 금지).
 * <p>테스트에서 Clock 을 바꿔 시간을 고정할 수 있고, 초 단위로 잘라 DB DATETIME 과 값이 같다.
 */
@Component
@RequiredArgsConstructor
public class TimeProvider {

    private final Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    public OffsetDateTime nowOffset() {
        return Kst.toOffset(now());
    }
}
