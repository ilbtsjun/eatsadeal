package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 변경 요청")
public record UpdatePassword(
        @Schema(description = "현재 비밀번호")
        @NotBlank
        String currentPassword,

        @Schema(description = "변경할 비밀번호")
        @NotBlank
        String updatePassword,

        @Schema(description = "변경할 비밀번호 확인")
        @NotBlank
        String passwordConfirm
) {
}
