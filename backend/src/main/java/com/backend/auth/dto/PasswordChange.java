package com.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "패스워드 재설정 요청")
public record PasswordChange(
        @Schema(description = "이메일", example = "user_temp@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "인증 코드")
        String authCode
) {
}
