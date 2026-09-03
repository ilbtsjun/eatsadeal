package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 탈퇴 요청")
public record QuitUser(
        @Schema(description = "비밀번호")
        String password
) {
}
