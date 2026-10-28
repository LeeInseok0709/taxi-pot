package com.taxipot.global.error;

import lombok.Getter;

import java.util.Map;

/**
 * 서비스에서 던지는 유일한 예외. GlobalExceptionHandler 가 ErrorResponse 로 바꾼다.
 * <pre>
 * throw new BusinessException(ErrorCode.NOT_POT_HOST);
 * throw new BusinessException(ErrorCode.INVALID_INPUT, "page 는 0 이상이어야 합니다.");
 * throw Errors.insufficientBalance(required, balance);   // detail 이 필요한 코드는 Errors 사용
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> detail;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> detail) {
        super(message == null ? errorCode.getDefaultMessage() : message);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
