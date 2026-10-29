package com.taxipot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * application.yml 의 {@code app.*} 설정값. 명세에 '서버 설정값'으로 적힌 숫자는 모두 여기로 모은다.
 * <p>코드에 15, 24, 50 같은 숫자를 직접 쓰지 말고 이 클래스에서 읽는다 (시연 프로필에서 시간을 줄이기 위함).
 * <p>사용: 생성자 주입 {@code private final AppProperties props;} → {@code props.pot().minLeadMinutes()}
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Crypto crypto,
        @DefaultValue Security security,
        @DefaultValue Upload upload,
        @DefaultValue Verification verification,
        @DefaultValue Pot pot,
        @DefaultValue Arrival arrival,
        @DefaultValue Debt debt,
        @DefaultValue FareClaim fareClaim,
        @DefaultValue Receipt receipt,
        @DefaultValue Wallet wallet,
        @DefaultValue Withdrawal withdrawal,
        @DefaultValue Rating rating,
        @DefaultValue Member member,
        @DefaultValue Scheduler scheduler,
        @DefaultValue Mail mail,
        @DefaultValue Admin admin
) {

    public record Jwt(
            /** HS256 서명 키 (32바이트 이상 문자열). 운영 값은 환경변수 TAXIPOT_JWT_SECRET */
            String secret,
            @DefaultValue("60") long accessTokenMinutes,
            @DefaultValue("14") long refreshTokenDays
    ) {
    }

    /** AES-256-GCM 키 (Base64 인코딩 32바이트). 운영 값은 환경변수 TAXIPOT_CRYPTO_KEY */
    public record Crypto(String aesKey) {
    }

    public record Security(
            /** true 면 X-Debug-User-Id 헤더로 로그인 없이 회원 지정 (local 프로필 전용, 운영 금지) */
            @DefaultValue("false") boolean debugHeaderEnabled,
            @DefaultValue({"http://localhost:3000", "http://localhost:5173"}) List<String> corsAllowedOrigins
    ) {
    }

    public record Upload(
            /** 업로드 파일 저장 루트 (외부 정적 공개 경로 아님). DB 에는 이 경로 기준 상대 경로만 저장 */
            @DefaultValue("./uploads") String dir
    ) {
    }

    public record Verification(
            /** 인증코드 유효 시간 (3.9.0: 1분) */
            @DefaultValue("1") long emailCodeTtlMinutes,
            /** 재발송 간격 (3.9.0: 60초). 직전 발송 시각 = email_code_expires_at − TTL */
            @DefaultValue("60") long resendIntervalSeconds,
            @DefaultValue("5") int emailCodeMaxAttempts,
            @DefaultValue("6") int emailCodeLength
    ) {
    }

    public record Pot(
            /** 방 생성: 출발 시각 ≥ 현재 + N분 (DEPARTURE_TIME_TOO_SOON) */
            @DefaultValue("15") long minLeadMinutes,
            /** 방 생성: 출발 시각 ≤ 현재 + N시간 (DEPARTURE_TIME_TOO_FAR) */
            @DefaultValue("24") long maxAdvanceHours,
            /** 집결 기한(= 참여 마감) = 출발 N분 전 */
            @DefaultValue("5") long arrivalDeadlineMinutes,
            /** 출발 N분 전부터 참여 취소·방 취소 불가 (CANCEL_TIME_EXPIRED) */
            @DefaultValue("10") long cancelDeadlineMinutes,
            /** 출발 N분 후에도 탑승 시작이 없으면 자동 취소 (스케줄러 3) */
            @DefaultValue("30") long autoCancelAfterMinutes,
            /** 전원 도착 N분 후 방장에게 출발 확인 알림 (스케줄러 11) */
            @DefaultValue("3") long startReminderMinutes,
            /** 지각 경고: 집결 기한 N분 전 미도착자에게 알림 (스케줄러 4, 명세에 N 미확정 → 3분으로 가정) */
            @DefaultValue("3") long lateWarningMinutes,
            /** 예치 여유율 1.2 = 12/10. 정수 계산 floor(expected_fare × 12 ÷ (max_members × 10)) */
            @DefaultValue("12") int holdBufferNumerator,
            @DefaultValue("10") int holdBufferDenominator,
            /** 서비스 수수료 (예치 시 포함, 기록·차감은 정산 시 탑승자에게만) */
            @DefaultValue("1000") int feeAmount,
            /** 미도착(노쇼) 정지 시간 */
            @DefaultValue("15") long noShowSuspendMinutes,
            /** 목록 조회 기본 반경 (m) */
            @DefaultValue("1000") int defaultRadiusMeters,
            /** 예상·실제 택시 요금 상한 */
            @DefaultValue("500000") int maxFare
    ) {
    }

    public record Arrival(
            /** 도착 인정 반경 (m) */
            @DefaultValue("50") int radiusMeters
    ) {
    }

    public record Debt(
            /** 미수금 납부 기한 = 정산 승인 시각 + N일 */
            @DefaultValue("3") long dueDays
    ) {
    }

    public record FareClaim(
            /** 탑승 시작 후 N시간 내 미청구 시 OVERDUE (스케줄러 9) */
            @DefaultValue("24") long deadlineHours
    ) {
    }

    public record Receipt(
            /** 영수증 사진 보관 기간 (스케줄러 10) */
            @DefaultValue("90") long retentionDays
    ) {
    }

    public record Wallet(
            /** balance + held_balance 상한 */
            @DefaultValue("500000") int maxBalance,
            @DefaultValue("10000") int minCharge,
            @DefaultValue("50000") int maxCharge
    ) {
    }

    public record Withdrawal(
            @DefaultValue("1000") int minAmount,
            /** 이 금액으로 출금하면 더미 송금이 실패한다 (시연용, 0 이면 사용 안 함) */
            @DefaultValue("0") int dummyFailAmount
    ) {
    }

    public record Rating(
            /** 정산 완료 후 평가 가능 기간 (이후 자동 기본 평가, 스케줄러 7) */
            @DefaultValue("24") long periodHours
    ) {
    }

    public record Member(
            /** 탈퇴 후 개인정보 파기까지 (스케줄러 6) */
            @DefaultValue("14") long withdrawnPurgeDays
    ) {
    }

    public record Scheduler(
            /** 스케줄러 전체 on/off. 각자 작업을 구현하기 전에는 false 로 둔다 */
            @DefaultValue("false") boolean enabled
    ) {
    }

    public record Mail(
            /** false 면 인증코드를 메일 대신 서버 로그로 출력 (로컬 개발용) */
            @DefaultValue("false") boolean enabled,
            @DefaultValue("noreply@taxipot.local") String from
    ) {
    }

    /** 운영자 계정 자동 생성 (local·demo). email 이 비어 있으면 생성하지 않는다 */
    public record Admin(String email, String password) {
    }
}
