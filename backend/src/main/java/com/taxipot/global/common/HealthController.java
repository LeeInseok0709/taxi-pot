package com.taxipot.global.common;

import com.taxipot.global.util.TimeUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/** 세팅 확인용. 응답 serverTime이 +09:00으로 나오면 시간대 설정까지 정상 */
@RestController
public class HealthController {

    public record HealthResponse(String status, OffsetDateTime serverTime) {}

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", TimeUtil.nowOffset());
    }
}
