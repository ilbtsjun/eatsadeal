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

    INVALID_STATUS(
            HttpStatus.CONFLICT,
            "COMMON-005",
            "상태 오류입니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "AUTH-001",
            "인증이 필요합니다."
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


    NOT_OWNED(
            HttpStatus.FORBIDDEN,
            "USER-001",
            "본인의 소유가 아닙니다."
    ),

    PASSWORD_NOT_MATCHED(
            HttpStatus.FORBIDDEN,
            "USER-002",
                    "비밀번호가 일치하지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
