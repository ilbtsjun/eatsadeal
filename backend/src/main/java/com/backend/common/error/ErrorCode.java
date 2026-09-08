package com.backend.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON-001",
            "잘못된 요청입니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON-002",
            "서버 내부 오류가 발생했습니다."
    ),

    ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "COMMON-003",
            "이미 사용중입니다."
    ),

    NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMON-004",
            "대상을 찾을 수 없습니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "AUTH-001",
            "인증이 필요합니다."
    ),

    INVALID_STATUS(
            HttpStatus.CONFLICT,
            "COMMON-005",
            "상태 오류입니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH-002",
            "유효하지 않은 토큰입니다."
    ),

    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "AUTH-003",
            "접근 권한이 없습니다."
    ),

    LOGIN_FAILED(
            HttpStatus.FORBIDDEN,
            "AUTH-004",
            "로그인에 실패했습니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER-001",
            "사용자를 찾을 수 없습니다."
    ),

    WITHDRAWN_USER(
            HttpStatus.UNAUTHORIZED,
            "USER-002",
            "탈퇴한 사용자입니다."
    ),

    SUSPENDED_USER(
            HttpStatus.FORBIDDEN,
            "USER-003",
            "정지된 사용자입니다."
    ),

    LOGIN_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "USER-004",
            "허용되지 않은 사용자입니다."
    ),

    NOT_OWNED(
            HttpStatus.FORBIDDEN,
            "USER-005",
            "본인의 소유가 아닙니다."
    ),

    EVENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "EVENT-001",
            "이벤트를 찾을 수 없습니다."
    ),

    EMAIL_VERIFICATION_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "EMAIL-001",
            "이메일 인증번호가 만료되었습니다."
    ),

    INVALID_EMAIL_VERIFICATION_CODE(
            HttpStatus.BAD_REQUEST,
            "EMAIL-002",
            "이메일 인증번호가 올바르지 않습니다."
    ),

    EMAIL_SEND_TOO_FREQUENT(
            HttpStatus.TOO_MANY_REQUESTS,
            "EMAIL-003",
            "이메일은 잠시 후 다시 요청할 수 있습니다."
    ),

    DUPLICATE_EMAIL(
            HttpStatus.CONFLICT,
            "EMAIL-004",
            "이미 사용 중인 이메일입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
