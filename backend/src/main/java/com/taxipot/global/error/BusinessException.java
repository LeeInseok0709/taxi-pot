package com.taxipot.global.error;

import lombok.Getter;

import java.util.Map;

/**
 * 서비스에서 실패를 알릴 때는 이 예외만 던진다. (RuntimeException·IllegalArgumentException 직접 금지 → 500이 됨)
 *
 * throw new BusinessException(ErrorCode.POT_FULL,
 *         ErrorDetail.of("maxMembers", 4, "currentMembers", 4));
 * throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
 *         "지갑 잔액이 %,d원 부족합니다.".formatted(shortage),
 *         ErrorDetail.of("requiredAmount", required, "balance", balance, "shortageAmount", shortage));
 *
 * 명세 'ErrorResponse.detail 규격'에 있는 코드(INSUFFICIENT_BALANCE, SUSPENDED_USER, POT_FULL 등 16개)는
 * detail을 반드시 채운다. 그 외 코드는 detail 없이 던진다 (→ "detail": null).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> detail;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> detail) {
        this(errorCode, errorCode.getMessage(), detail);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> detail) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
