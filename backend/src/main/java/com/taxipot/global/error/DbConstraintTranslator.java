package com.taxipot.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DB 제약 위반 → 명세 에러 코드 변환 (명세: CHECK 위반을 500 으로 내보내지 말 것).
 * <ul>
 *   <li>UNIQUE 위반(1062): 동시 요청으로 사전 검사를 통과한 경우 → DUPLICATE_* 등</li>
 *   <li>CHECK 위반(3819): 서버 검증 누락 = 버그. ERROR 로그를 남기고 의미에 맞는 409/400 으로 변환</li>
 * </ul>
 * 새 UNIQUE 키를 추가하면 UNIQUE_KEYS 에 등록한다.
 */
@Slf4j
@Component
public class DbConstraintTranslator {

    private static final Pattern DUPLICATE_KEY = Pattern.compile("for key '([^']+)'");
    private static final Pattern CHECK_NAME = Pattern.compile("[Cc]heck constraint '([^']+)'");

    private static final Map<String, ErrorCode> UNIQUE_KEYS = Map.of(
            "uk_users_email", ErrorCode.DUPLICATE_EMAIL,
            "uk_users_nickname", ErrorCode.DUPLICATE_NICKNAME,
            "uk_users_student_no", ErrorCode.DUPLICATE_STUDENT_NO,
            "uk_tpm_pot_member", ErrorCode.ALREADY_JOINED,
            "uk_rating_pair", ErrorCode.ALREADY_RATED,
            "uk_report_dup", ErrorCode.DUPLICATE_REPORT,
            "uk_payment_order_id", ErrorCode.PAYMENT_ALREADY_PROCESSED,
            "uk_payment_pg_tid", ErrorCode.PAYMENT_ALREADY_PROCESSED,
            "uk_settle_pot_member", ErrorCode.ALREADY_JOINED
    );

    public BusinessException translate(DataIntegrityViolationException e) {
        SQLException sql = findSqlException(e);
        String message = sql != null ? sql.getMessage() : String.valueOf(e.getMostSpecificCause().getMessage());
        int vendorCode = sql != null ? sql.getErrorCode() : 0;

        Matcher dup = DUPLICATE_KEY.matcher(message);
        if (vendorCode == 1062 || dup.find()) {
            String key = dup.find(0) ? lastSegment(dup.group(1)) : "";
            ErrorCode code = UNIQUE_KEYS.get(key);
            if (code != null) {
                log.info("UNIQUE 위반 → {} ({})", code, key);
                return new BusinessException(code);
            }
            log.error("등록되지 않은 UNIQUE 위반: {}", message);
            return new BusinessException(ErrorCode.INVALID_INPUT, "중복된 값입니다.");
        }

        Matcher check = CHECK_NAME.matcher(message);
        if (vendorCode == 3819 || check.find()) {
            String name = check.find(0) ? check.group(1) : "";
            log.error("[서버 검증 누락] DB CHECK 위반 {} — 서비스에서 먼저 검사해야 합니다: {}", name, message);
            return new BusinessException(checkToCode(name));
        }

        if (vendorCode == 1452) {
            log.error("FK 위반: {}", message);
            return Errors.notFound("참조 대상");
        }
        log.error("DB 무결성 위반: {}", message, e);
        return new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private static ErrorCode checkToCode(String name) {
        if (name.equals("chk_users_nickname_reserved")) {
            return ErrorCode.RESERVED_NICKNAME;
        }
        if (name.startsWith("chk_taxipot") || name.startsWith("chk_settlement")) {
            return ErrorCode.INVALID_POT_STATUS;
        }
        if (name.startsWith("chk_payment")) {
            return ErrorCode.INVALID_PAYMENT_STATUS;
        }
        if (name.startsWith("chk_wallet") || name.equals("chk_point_history_balance_after_rng")) {
            return ErrorCode.INSUFFICIENT_BALANCE;
        }
        return ErrorCode.INVALID_INPUT;
    }

    private static String lastSegment(String key) {
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(dot + 1) : key;
    }

    private static SQLException findSqlException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException s) {
                return s;
            }
            t = t.getCause();
        }
        return null;
    }
}
