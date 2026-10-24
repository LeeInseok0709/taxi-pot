package com.taxipot.global.error;

import com.taxipot.global.code.EmailStatus;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.ReviewStatus;
import com.taxipot.global.time.Kst;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 명세 'ErrorResponse.detail 규격'에서 detail 을 반드시 채워야 하는 코드의 예외 생성기.
 * <p>이 코드들은 {@code new BusinessException(...)} 으로 직접 만들지 말고 반드시 여기 메서드를 쓴다
 * (detail 필드 이름·형식이 명세와 어긋나면 앱 팝업 문구가 깨진다).
 * <p>LocalDateTime 인자는 KST 기준이며 응답에서는 +09:00 문자열로 나간다.
 */
public final class Errors {

    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Errors() {
    }

    public static BusinessException notFound(String target) {
        return new BusinessException(ErrorCode.NOT_FOUND, target + "을(를) 찾을 수 없습니다.");
    }

    public static BusinessException invalidInput(String message) {
        return new BusinessException(ErrorCode.INVALID_INPUT, message);
    }

    /** 409 INSUFFICIENT_BALANCE: requiredAmount, balance, shortageAmount */
    public static BusinessException insufficientBalance(int requiredAmount, int balance) {
        int shortage = Math.max(requiredAmount - balance, 0);
        return new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                String.format("지갑 잔액이 %,d원 부족합니다.", shortage),
                ErrorDetail.of("requiredAmount", requiredAmount, "balance", balance, "shortageAmount", shortage));
    }

    /** 403 SUSPENDED_USER: suspendedUntil (영구 정지면 null). 정지 사유는 넣지 않는다 */
    public static BusinessException suspended(LocalDateTime suspendedUntil) {
        String message = suspendedUntil == null
                ? "이용이 정지된 계정입니다."
                : "이용 정지 중입니다. " + suspendedUntil.format(MESSAGE_TIME) + " 이후 다시 시도해 주세요.";
        return new BusinessException(ErrorCode.SUSPENDED_USER, message,
                ErrorDetail.of("suspendedUntil", Kst.toOffset(suspendedUntil)));
    }

    /** 400 OUT_OF_ARRIVAL_RANGE: distanceMeters, allowedRadiusMeters */
    public static BusinessException outOfArrivalRange(int distanceMeters, int allowedRadiusMeters) {
        return new BusinessException(ErrorCode.OUT_OF_ARRIVAL_RANGE,
                String.format("약속 장소에서 %,dm 떨어져 있습니다.", distanceMeters),
                ErrorDetail.of("distanceMeters", distanceMeters, "allowedRadiusMeters", allowedRadiusMeters));
    }

    /** 409 CANCEL_TIME_EXPIRED: cancelDeadline */
    public static BusinessException cancelTimeExpired(LocalDateTime cancelDeadline) {
        return new BusinessException(ErrorCode.CANCEL_TIME_EXPIRED, ErrorCode.CANCEL_TIME_EXPIRED.getDefaultMessage(),
                ErrorDetail.of("cancelDeadline", Kst.toOffset(cancelDeadline)));
    }

    /** 409 ARRIVAL_DEADLINE_PASSED: arrivalDeadline */
    public static BusinessException arrivalDeadlinePassed(LocalDateTime arrivalDeadline) {
        return new BusinessException(ErrorCode.ARRIVAL_DEADLINE_PASSED, ErrorCode.ARRIVAL_DEADLINE_PASSED.getDefaultMessage(),
                ErrorDetail.of("arrivalDeadline", Kst.toOffset(arrivalDeadline)));
    }

    /** 409 ACTIVE_POT_EXISTS: activePotId */
    public static BusinessException activePotExists(Long activePotId) {
        return new BusinessException(ErrorCode.ACTIVE_POT_EXISTS, ErrorCode.ACTIVE_POT_EXISTS.getDefaultMessage(),
                ErrorDetail.of("activePotId", activePotId));
    }

    /** 403 GENDER_RESTRICTED: genderOption */
    public static BusinessException genderRestricted(GenderOption genderOption) {
        String message = genderOption == GenderOption.FEMALE ? "여성만 참여할 수 있는 방입니다."
                : genderOption == GenderOption.MALE ? "남성만 참여할 수 있는 방입니다."
                : ErrorCode.GENDER_RESTRICTED.getDefaultMessage();
        return new BusinessException(ErrorCode.GENDER_RESTRICTED, message, ErrorDetail.of("genderOption", genderOption));
    }

    /** 409 POT_FULL: maxMembers, currentMembers */
    public static BusinessException potFull(int maxMembers, int currentMembers) {
        return new BusinessException(ErrorCode.POT_FULL, ErrorCode.POT_FULL.getDefaultMessage(),
                ErrorDetail.of("maxMembers", maxMembers, "currentMembers", currentMembers));
    }

    /** 403 NOT_VERIFIED: emailStatus, reviewStatus (가장 최근 인증 행 기준, 없으면 null) */
    public static BusinessException notVerified(EmailStatus emailStatus, ReviewStatus reviewStatus) {
        return new BusinessException(ErrorCode.NOT_VERIFIED, ErrorCode.NOT_VERIFIED.getDefaultMessage(),
                ErrorDetail.of("emailStatus", emailStatus, "reviewStatus", reviewStatus));
    }

    /** 409 WALLET_LIMIT_EXCEEDED: maxBalance, availableAmount */
    public static BusinessException walletLimitExceeded(int maxBalance, int availableAmount) {
        return new BusinessException(ErrorCode.WALLET_LIMIT_EXCEEDED,
                String.format("지갑에는 최대 %,d원까지 보관할 수 있습니다.", maxBalance),
                ErrorDetail.of("maxBalance", maxBalance, "availableAmount", Math.max(availableAmount, 0)));
    }

    /** 400 INVALID_DATETIME_FORMAT: field, value */
    public static BusinessException invalidDatetimeFormat(String field, Object value) {
        return new BusinessException(ErrorCode.INVALID_DATETIME_FORMAT, ErrorCode.INVALID_DATETIME_FORMAT.getDefaultMessage(),
                ErrorDetail.of("field", field, "value", value == null ? null : String.valueOf(value)));
    }

    /** 400 INVALID_EMAIL_CODE: remainingAttempts (0 이면 재발송 필요) */
    public static BusinessException invalidEmailCode(int remainingAttempts) {
        return new BusinessException(ErrorCode.INVALID_EMAIL_CODE,
                remainingAttempts > 0
                        ? String.format("인증코드가 일치하지 않습니다. (남은 횟수 %d회)", remainingAttempts)
                        : "인증코드가 일치하지 않습니다. 인증코드를 다시 발송해 주세요.",
                ErrorDetail.of("remainingAttempts", Math.max(remainingAttempts, 0)));
    }

    /** 409 JOIN_DEADLINE_PASSED: joinDeadline → 앱은 '입장 요청' 버튼으로 전환 */
    public static BusinessException joinDeadlinePassed(LocalDateTime joinDeadline) {
        return new BusinessException(ErrorCode.JOIN_DEADLINE_PASSED, ErrorCode.JOIN_DEADLINE_PASSED.getDefaultMessage(),
                ErrorDetail.of("joinDeadline", Kst.toOffset(joinDeadline)));
    }

    /** 400 DEPARTURE_TIME_TOO_SOON: minDepartureTime */
    public static BusinessException departureTimeTooSoon(LocalDateTime minDepartureTime, long minLeadMinutes) {
        return new BusinessException(ErrorCode.DEPARTURE_TIME_TOO_SOON,
                String.format("출발 시각은 현재로부터 %d분 이후여야 합니다.", minLeadMinutes),
                ErrorDetail.of("minDepartureTime", Kst.toOffset(minDepartureTime)));
    }

    /** 400 DEPARTURE_TIME_TOO_FAR: maxDepartureTime */
    public static BusinessException departureTimeTooFar(LocalDateTime maxDepartureTime, long maxAdvanceHours) {
        return new BusinessException(ErrorCode.DEPARTURE_TIME_TOO_FAR,
                String.format("출발 시각은 현재로부터 %d시간 이내여야 합니다.", maxAdvanceHours),
                ErrorDetail.of("maxDepartureTime", Kst.toOffset(maxDepartureTime)));
    }

    /** 409 UNPAID_DEBT_EXISTS: debtAmount(합계), debtDueAt(가장 이른 납부 기한, 연체만 있으면 null) */
    public static BusinessException unpaidDebtExists(int debtAmount, LocalDateTime earliestDueAt) {
        return new BusinessException(ErrorCode.UNPAID_DEBT_EXISTS,
                String.format("미납 금액 %,d원을 먼저 납부해 주세요.", debtAmount),
                ErrorDetail.of("debtAmount", debtAmount, "debtDueAt", Kst.toOffset(earliestDueAt)));
    }
}
