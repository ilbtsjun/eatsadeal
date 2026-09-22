package com.backend.common.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final List<CustomFieldError> errors;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode, String customMessage) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(customMessage)
                        .build());
    }

    @Getter
    @Builder
    public static class CustomFieldError {
        private String field;
        private String value;
        private String reason;
    }

    public String toJson() {
        return String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"errors\":null}",
                this.code,
                this.message
        );
    }
}