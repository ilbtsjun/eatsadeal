package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
    @Schema(description = "유저 닉네임 또는 이메일", example = "김삿갓, user_temp@gmail.com")
    @NotBlank(message = "아이디는 필수입니다.")
    String id,

    @Schema(description = "비밀번호", example = "password1234")
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password
) {
}
