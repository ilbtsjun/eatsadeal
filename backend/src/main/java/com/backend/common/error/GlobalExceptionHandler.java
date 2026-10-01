package com.backend.common.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "pwd", "secret", "token", "code"
    );
    private static final String MASK = "******";

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if(errorCode.getStatus().is5xxServerError()) {
            log.error("BusinessException [{}]: {}", errorCode.getCode(), e.getMessage(), e);
        }
        else{
            log.warn("BusinessException [{}]: {}", errorCode.getCode(), e.getMessage());
        }
        return ErrorResponse.toResponseEntity(errorCode, e.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.warn("Parameter validation failed: {}", e.getMessage());
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());

        BindingResult bindingResult = e.getBindingResult();
        List<ErrorResponse.CustomFieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> ErrorResponse.CustomFieldError.builder()
                        .field(error.getField())
                        .value(error.getRejectedValue() == null ? "" : error.getRejectedValue().toString())
                        .reason(error.getDefaultMessage())
                        .build())
                .toList();

        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(ErrorCode.INVALID_REQUEST.getMessage())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus()).body(response);
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        List<ErrorResponse.CustomFieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toSafeFieldError)
                .toList();

        log.warn("Validation failed: {}", fieldErrors.stream()
                .map(f -> f.getField() + "(" + f.getReason() + ")")
                .toList());

        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(ErrorCode.INVALID_REQUEST.getMessage())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body");
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: param={}", e.getName());
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST, "'" + e.getName() + "' 값의 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_REQUEST, "필수 파라미터 '" + e.getParameterName() + "'이(가) 없습니다.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMethod());
        return ErrorResponse.toResponseEntity(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        log.warn("No resource: {}", e.getResourcePath());
        return ErrorResponse.toResponseEntity(ErrorCode.NOT_FOUND, "요청한 경로를 찾을 수 없습니다.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null
                || auth instanceof AnonymousAuthenticationToken
                || !auth.isAuthenticated();
        if (anonymous) {
            log.warn("Access denied (unauthenticated)");
            return ErrorResponse.toResponseEntity(ErrorCode.UNAUTHORIZED);
        }
        log.warn("Access denied: user={}", auth.getName());
        return ErrorResponse.toResponseEntity(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getClass().getSimpleName());
        return ErrorResponse.toResponseEntity(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse.CustomFieldError toSafeFieldError(FieldError error) {
        String field = error.getField();
        Object rejected = error.getRejectedValue();
        String value;
        if(isSensitive(field)){
            value = MASK;
        }
        else if(rejected == null){
            value = "";
        }
        else{
            String raw = rejected.toString();
            value = raw.length() > 100 ? raw.substring(0, 100) + "..." : raw;
        }
        return ErrorResponse.CustomFieldError.builder()
                .field(field)
                .value(value)
                .reason(error.getDefaultMessage())
                .build();
    }

    private boolean isSensitive(String field) {
        if(field == null){
            return false;
        }
        String lower = field.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }
}