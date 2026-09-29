package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 검색 요청")
public record GetUserInfoRequest(
        @Schema(description = "ID")
        Long id,

        @Schema(description = "닉네임")
        String nickname
) {
}
