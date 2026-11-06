package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "유저 탈퇴 요청")
public record QuitUser(
        @Schema(description = "비밀번호")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
