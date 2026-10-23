package com.taxipot.global.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 모든 실패를 명세 ErrorResponse 형식으로 바꾼다. 팀원은 이 파일을 수정하지 않는다.
 * 주의: Security 필터(JWT) 안에서 난 예외는 여기까지 오지 않는다 → SecurityErrorHandler가 처리.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.info("[{}] {}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage(), e.getDetail());
    }

    /** @Valid 요청 바디 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValid(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField).distinct().collect(Collectors.joining(", "));
        return build(ErrorCode.INVALID_INPUT, "잘못된 입력값입니다: " + fields, null);
    }

    /** @RequestParam·@PathVariable에 붙인 @Min 등 검증 실패 (Spring 6.1+) */
    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleParamValid(Exception e) {
        return build(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), null);
    }

    /** JSON 파싱 실패. 일시 필드에 +09:00이 없으면 INVALID_DATETIME_FORMAT(field, value) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException mie
                && mie.getTargetType() == OffsetDateTime.class) {
            String field = mie.getPath().isEmpty() ? null : mie.getPath().get(mie.getPath().size() - 1).getFieldName();
            Object value = (mie instanceof InvalidFormatException ife) ? ife.getValue() : null;
            return build(ErrorCode.INVALID_DATETIME_FORMAT, ErrorCode.INVALID_DATETIME_FORMAT.getMessage(),
                    ErrorDetail.of("field", field, "value", value));
        }
        return build(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다.", null);
    }

    /** 쿼리·경로 파라미터 타입 오류 (예: from=2026-10-16T23:00:00 처럼 오프셋 없음, potId=abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getRequiredType() == OffsetDateTime.class) {
            return build(ErrorCode.INVALID_DATETIME_FORMAT, ErrorCode.INVALID_DATETIME_FORMAT.getMessage(),
                    ErrorDetail.of("field", e.getName(), "value", e.getValue()));
        }
        return build(ErrorCode.INVALID_INPUT, "잘못된 파라미터입니다: " + e.getName(), null);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MissingRequestHeaderException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleMissing(Exception e) {
        return build(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e) {
        return build(ErrorCode.INVALID_INPUT, "파일은 10MB 이하만 올릴 수 있습니다.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage(), null);
    }

    /**
     * 명세: DB CHECK 위반(MySQL 3819)을 500으로 내보내지 말 것. 정상이라면 서비스 검증이 먼저 막아야 하므로
     * 여기 걸리면 '서버 검증 누락' 버그다 → error 로그를 꼭 확인하고 서비스에 검증을 추가한다.
     * - 1062 중복 키(동시에 두 번 누름 등) → 409 ALREADY_PROCESSED
     * - 3819 CHECK 위반 → 409 INVALID_POT_STATUS (명세 예시)
     * - 그 외(FK 등) → 400 INVALID_INPUT
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException e) {
        int vendorCode = findVendorCode(e);
        log.error("DB 제약 위반 (MySQL {}) — 서비스 검증 누락 확인 필요", vendorCode, e);
        ErrorCode code = switch (vendorCode) {
            case 1062 -> ErrorCode.ALREADY_PROCESSED;
            case 3819 -> ErrorCode.INVALID_POT_STATUS;
            default -> ErrorCode.INVALID_INPUT;
        };
        return build(code, code.getMessage(), null);
    }

    /**
     * 컨트롤러·서비스 안에서 난 Security 예외. 아래 Exception 핸들러가 먼저 잡아 500이 되는 것을 막는다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage(), null);
    }

    /** 자동 생성된 서비스 메서드 중 아직 구현 안 된 것 (TODO) */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotImplemented(UnsupportedOperationException e) {
        log.warn("미구현 API: {}", e.getMessage());
        return build(ErrorCode.INTERNAL_SERVER_ERROR, "아직 구현되지 않은 API입니다: " + e.getMessage(), null);
    }

    /** 예상 못 한 오류는 400이 아니라 500. (400으로 숨기면 서버 버그를 앱 입력 오류로 착각하게 된다) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return build(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, Map<String, Object> detail) {
        return ResponseEntity.status(code.getHttpStatus()).body(ErrorResponse.of(code, message, detail));
    }

    private int findVendorCode(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sql) {
                return sql.getErrorCode();
            }
            t = t.getCause();
        }
        return -1;
    }
}
