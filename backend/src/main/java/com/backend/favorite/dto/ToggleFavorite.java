package com.backend.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "즐겨찾기 토글 요청")
public record ToggleFavorite(
        @Schema(description = "유저 ID", example = "1")
        Long userId,

        @Schema(description = "이벤트 ID", example = "1")
        Long eventId
) {
}
