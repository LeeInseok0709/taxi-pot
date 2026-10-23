package com.taxipot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * application.yml의 app.* 설정값 전부. 숫자(15분, 50m, 1.2배...)를 코드에 직접 쓰지 말고 여기서 꺼내 쓴다.
 * → 시연 프로필(application-demo.yml)에서 시간 값만 줄여서 시연할 수 있다.
 * 사용: 생성자 주입 후 appProperties.pot().minLeadMinutes()
 * 항목 추가·변경은 기성만.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String uploadDir,
        Jwt jwt,
        Crypto crypto,
        Auth auth,
        Cors cors,
        Scheduler scheduler,
        Verification verification,
        Pot pot,
        Arrival arrival,
        Penalty penalty,
        Debt debt,
        FareClaim fareClaim,
        Receipt receipt,
        Withdrawal withdrawal,
        Wallet wallet,
        Rating rating,
        User user
) {
    public record Jwt(String secret, long accessTokenSeconds, long refreshTokenSeconds) {}

    /** aesKey: Base64로 인코딩된 32바이트 키 (AES-256-GCM). 깃에 올리지 않는다. */
    public record Crypto(String aesKey) {}

    /** debugHeaderEnabled: JWT 완성 전 개발용. true면 X-Debug-User-Id 헤더로 로그인 사용자 지정 (local 전용) */
    public record Auth(boolean debugHeaderEnabled) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Scheduler(boolean enabled) {}

    /** 명세 3.9.0: emailCodeTtlMinutes=1, resendIntervalSeconds=60, 입력 실패 5회 */
    public record Verification(int emailCodeTtlMinutes, int resendIntervalSeconds, int maxCodeAttempts) {}

    /**
     * holdBufferRate=1.2(3.7.0), minLeadMinutes=15, maxAdvanceHours=24, startReminderMinutes=3,
     * cancelDeadlineMinutes=10(출발 10분 전 취소 불가), arrivalDeadlineMinutes=5(집결 기한 = 출발 5분 전),
     * autoCancelMinutes=30(출발 30분 후 자동 취소), fee=1000(수수료)
     */
    public record Pot(BigDecimal holdBufferRate, int minLeadMinutes, int maxAdvanceHours, int startReminderMinutes,
                      int cancelDeadlineMinutes, int arrivalDeadlineMinutes, int autoCancelMinutes, int fee) {}

    /** radiusMeters=50, lateWarningMinutes=집결 기한 N분 전 지각 경고 */
    public record Arrival(int radiusMeters, int lateWarningMinutes) {}

    /** noShowSuspendMinutes=15 */
    public record Penalty(int noShowSuspendMinutes) {}

    /** dueDays=3 */
    public record Debt(int dueDays) {}

    /** deadlineHours=24 */
    public record FareClaim(int deadlineHours) {}

    /** retentionDays=90 */
    public record Receipt(int retentionDays) {}

    /** dummyFailAmount: 이 금액으로 출금 요청하면 더미 송금 실패 (시연용) */
    public record Withdrawal(int dummyFailAmount, int minAmount) {}

    /** maxBalance=1,000,000, chargeMin=1,000, chargeMax=500,000, maxFare=500,000 */
    public record Wallet(int maxBalance, int chargeMin, int chargeMax, int maxFare) {}

    /** periodHours=24 (정산 후 평가 가능 시간) */
    public record Rating(int periodHours) {}

    /** purgeDays=14 (탈퇴 후 개인정보 파기) */
    public record User(int purgeDays) {}
}
