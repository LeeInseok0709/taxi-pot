package com.taxipot.global.web;

import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 실행 확인용 API (명세 외, 서버 전용). 로그인 없이 호출 가능.
 * <p>팀원이 레포를 받아 서버를 띄운 뒤 Swagger 에서 이 API 가 200 이면 세팅이 끝난 것이다.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final TimeProvider timeProvider;

    /** GET /api/health → {"status":"UP","serverTime":"2026-10-30T19:30:00+09:00"} */
    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", Kst.toOffset(timeProvider.now()));
    }

    public record HealthResponse(String status, OffsetDateTime serverTime) {
    }
}
