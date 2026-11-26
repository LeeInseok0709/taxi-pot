package com.taxipot.global.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.taxipot.global.time.TimeProvider;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 모든 예외 → ErrorResponse. 팀원은 여기를 고치지 않고 BusinessException 만 던진다.
 * <p>모르는 예외는 400 이 아니라 500 INTERNAL_ERROR (서버 버그가 앱 입력 오류처럼 보이지 않게).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TimeProvider timeProvider;
    private final DbConstraintTranslator dbConstraintTranslator;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.NOT_IMPLEMENTED) {
            log.warn(e.getMessage());
        }
        return respond(e.getErrorCode(), e.getMessage(), e.getDetail());
    }

    /** @Valid @RequestBody 검증 실패, @ModelAttribute(multipart) 바인딩·검증 실패 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException e) {
        FieldError typeMismatch = e.getFieldErrors().stream()
                .filter(f -> f.isBindingFailure() && "typeMismatch".equals(f.getCode()))
                .findFirst().orElse(null);
        if (typeMismatch != null && isDateTimeField(e, typeMismatch.getField())) {
            return respond(Errors.invalidDatetimeFormat(typeMismatch.getField(), typeMismatch.getRejectedValue()));
        }
        String message = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(ErrorCode.INVALID_INPUT, message.isBlank() ? null : message, null);
    }

    /** @RequestParam·@PathVariable 의 @Min 등 검증 실패 (Spring 6.1+) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        String message = e.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(err -> r.getMethodParameter().getParameterName() + ": " + err.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return respond(ErrorCode.INVALID_INPUT, message.isBlank() ? null : message, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return respond(ErrorCode.INVALID_INPUT, message, null);
    }

    /** JSON 파싱 실패. 일시 형식 오류는 INVALID_DATETIME_FORMAT, 나머지는 INVALID_INPUT */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() == OffsetDateTime.class) {
            return respond(Errors.invalidDatetimeFormat(pathOf(ife), ife.getValue()));
        }
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            return respond(ErrorCode.INVALID_INPUT, pathOf(ife) + ": 허용되지 않는 값입니다 (" + ife.getValue() + ")", null);
        }
        if (cause instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            return respond(ErrorCode.INVALID_INPUT, pathOf(jme) + ": 형식이 올바르지 않습니다.", null);
        }
        return respond(ErrorCode.INVALID_INPUT, "요청 본문(JSON) 형식이 올바르지 않습니다.", null);
    }

    /** 쿼리·경로 파라미터 타입 오류 (예: from=2026-10-16T23:00:00, status=WRONG) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getRequiredType() == OffsetDateTime.class) {
            return respond(Errors.invalidDatetimeFormat(e.getName(), e.getValue()));
        }
        return respond(ErrorCode.INVALID_INPUT, e.getName() + ": 허용되지 않는 값입니다 (" + e.getValue() + ")", null);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MissingRequestHeaderException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleMissing(Exception e) {
        return respond(ErrorCode.INVALID_INPUT, e.getMessage(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e) {
        return respond(ErrorCode.INVALID_INPUT, "파일은 10MB 이하만 올릴 수 있습니다.", null);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
        return respond(ErrorCode.INVALID_INPUT, "multipart/form-data 요청 형식이 올바르지 않습니다.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        return respond(dbConstraintTranslator.translate(e));
    }

    /** 컨트롤러 안에서 발생한 권한 예외 (필터 단계 예외는 RestAccessDeniedHandler 가 처리) */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception e) {
        return respond(ErrorCode.FORBIDDEN, null, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, null, null);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandler(Exception e) {
        return respond(ErrorCode.NOT_FOUND, "존재하지 않는 API 입니다.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR, null, null);
    }

    private boolean isDateTimeField(BindException e, String field) {
        Class<?> type = e.getBindingResult().getFieldType(field);
        return type == OffsetDateTime.class;
    }

    private static String pathOf(JsonMappingException e) {
        return e.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : "[" + r.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private ResponseEntity<ErrorResponse> respond(BusinessException e) {
        return respond(e.getErrorCode(), e.getMessage(), e.getDetail());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, Map<String, Object> detail) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, message, detail, timeProvider.nowOffset()));
    }
}
