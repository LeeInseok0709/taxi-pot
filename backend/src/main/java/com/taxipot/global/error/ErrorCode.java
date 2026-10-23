package com.taxipot.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 명세서 3.9.0 '공통 에러 코드' 77개 전부 + 서버 전용 2개. 수정은 기성만.
 * - 응답 JSON의 code = enum 이름. 팀원은 throw new BusinessException(ErrorCode.XXX)만 쓴다.
 * - message는 기본 문구. 금액·거리처럼 값이 들어가는 문구는 BusinessException의 message 인자로 덮어쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 400 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_DATETIME_FORMAT(HttpStatus.BAD_REQUEST, "일시는 +09:00 오프셋을 포함해야 합니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해 주세요."),
    INVALID_EMAIL_CODE(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않습니다."),
    EXPIRED_EMAIL_CODE(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 발송해 주세요."),
    STUDENT_NO_MISMATCH(HttpStatus.BAD_REQUEST, "학번이 일치하지 않습니다."),
    OUT_OF_ARRIVAL_RANGE(HttpStatus.BAD_REQUEST, "약속 장소 반경 밖에 있습니다."),
    BAD_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "별로였던 이유를 입력해 주세요."),
    SELF_RATING_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 평가할 수 없습니다."),
    SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 신고할 수 없습니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유를 입력해 주세요."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
    INVALID_QUICK_MESSAGE(HttpStatus.BAD_REQUEST, "사용할 수 없는 퀵블록 메시지입니다."),
    RESERVED_NICKNAME(HttpStatus.BAD_REQUEST, "이후 사용할 수 없는 닉네임입니다."),
    UNSUPPORTED_UNIVERSITY(HttpStatus.BAD_REQUEST, "가입할 수 없는 학교입니다."),
    EMAIL_DOMAIN_MISMATCH(HttpStatus.BAD_REQUEST, "학교 이메일 도메인이 일치하지 않습니다."),
    DUMMY_CHARGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "앱에서는 더미 충전을 할 수 없습니다."),
    DEPARTURE_TIME_TOO_SOON(HttpStatus.BAD_REQUEST, "출발 시각은 현재로부터 15분 이후여야 합니다."),
    RECEIPT_REQUIRED(HttpStatus.BAD_REQUEST, "결제내역 사진을 첨부해 주세요."),
    BALANCE_FORFEIT_NOT_AGREED(HttpStatus.BAD_REQUEST, "잔액 소멸에 동의해야 탈퇴할 수 있습니다."),
    DEPARTURE_TIME_TOO_FAR(HttpStatus.BAD_REQUEST, "출발 시각은 현재로부터 24시간 이내여야 합니다."),
    ACCOUNT_HOLDER_MISMATCH(HttpStatus.BAD_REQUEST, "예금주가 가입자 이름과 일치하지 않습니다."),

    // ===== 401 =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 토큰이 없거나 만료되었습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요."),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),

    // ===== 403 =====
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),
    NOT_VERIFIED(HttpStatus.FORBIDDEN, "학생 인증이 완료되지 않았습니다."),
    SUSPENDED_USER(HttpStatus.FORBIDDEN, "이용 정지 중입니다."),
    GENDER_RESTRICTED(HttpStatus.FORBIDDEN, "성별 제한으로 참여할 수 없는 방입니다."),
    NOT_POT_HOST(HttpStatus.FORBIDDEN, "방장만 할 수 있습니다."),
    NOT_POT_MEMBER(HttpStatus.FORBIDDEN, "택시팟 참여자가 아닙니다."),
    KICKED_FROM_POT(HttpStatus.FORBIDDEN, "내보내진 택시팟입니다."),
    HOST_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "방장은 참여 취소 대신 방을 취소해야 합니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증을 먼저 완료해 주세요."),
    REPORT_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "신고할 수 있는 대상이 아닙니다."),
    JOIN_REQUEST_REJECTED(HttpStatus.FORBIDDEN, "입장 요청이 거절된 방입니다."),

    // ===== 404 =====
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),

    // ===== 409 =====
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_STUDENT_NO(HttpStatus.CONFLICT, "이미 가입된 학번입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료되었습니다."),
    REVIEW_IN_PROGRESS(HttpStatus.CONFLICT, "심사가 진행 중입니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "지갑 잔액이 부족합니다."),
    ACTIVE_POT_EXISTS(HttpStatus.CONFLICT, "이미 참여 중인 택시팟이 있습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 택시팟입니다."),
    POT_FULL(HttpStatus.CONFLICT, "모집 인원이 모두 찼습니다."),
    INVALID_POT_STATUS(HttpStatus.CONFLICT, "지금은 처리할 수 없는 택시팟 상태입니다."),
    CANCEL_TIME_EXPIRED(HttpStatus.CONFLICT, "출발 10분 전부터는 취소할 수 없습니다."),
    ALREADY_ARRIVED(HttpStatus.CONFLICT, "이미 도착 확인을 했습니다."),
    ARRIVAL_DEADLINE_PASSED(HttpStatus.CONFLICT, "집결 기한이 지났습니다."),
    MEMBER_ALREADY_ARRIVED(HttpStatus.CONFLICT, "이미 도착한 참여자는 내보낼 수 없습니다."),
    NOT_ALL_ARRIVED(HttpStatus.CONFLICT, "아직 도착하지 않은 참여자가 있습니다."),
    NOT_ENOUGH_MEMBERS(HttpStatus.CONFLICT, "2명 이상이어야 출발할 수 있습니다."),
    ALREADY_SETTLED(HttpStatus.CONFLICT, "이미 정산되었습니다."),
    ALREADY_RATED(HttpStatus.CONFLICT, "이미 평가했습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "이미 신고했습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 심사가 끝났습니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "처리할 수 없는 결제 상태입니다."),
    WALLET_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "지갑에는 최대 1,000,000원까지 보관할 수 있습니다."),
    JOIN_DEADLINE_PASSED(HttpStatus.CONFLICT, "참여 마감 시각이 지났습니다. 출발지 근처에서 입장 요청을 보내 주세요."),
    JOIN_REQUEST_NOT_AVAILABLE(HttpStatus.CONFLICT, "지금은 입장 요청을 할 수 없습니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 입장 요청을 보냈습니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.CONFLICT, "입장 요청을 찾을 수 없습니다."),
    UNPAID_DEBT_EXISTS(HttpStatus.CONFLICT, "미납 금액을 먼저 납부해 주세요."),
    NO_DEBT(HttpStatus.CONFLICT, "납부할 미수금이 없습니다."),
    CAPTURE_NOT_SUBMITTED(HttpStatus.CONFLICT, "캡처본이 제출되지 않았습니다."),
    EMAIL_CODE_ATTEMPTS_EXCEEDED(HttpStatus.CONFLICT, "입력 횟수를 초과했습니다. 인증코드를 다시 발송해 주세요."),
    RATING_PERIOD_EXPIRED(HttpStatus.CONFLICT, "평가 기간이 지났습니다."),
    ACCOUNT_NOT_REGISTERED(HttpStatus.CONFLICT, "출금 계좌를 먼저 등록해 주세요."),
    FARE_CLAIM_IN_REVIEW(HttpStatus.CONFLICT, "운영자가 확인 중인 청구입니다."),
    FARE_CLAIM_NOT_PENDING(HttpStatus.CONFLICT, "확인 대기 중인 청구가 아닙니다."),
    PAYMENT_CHECK_FAILED(HttpStatus.CONFLICT, "결제 확인에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    // ===== 429 =====
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요."),

    // ===== 서버 전용 (명세 '공통 에러 코드'에 추가할 것) =====
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return name();
    }
}
