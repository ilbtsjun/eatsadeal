package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이벤트 숨김 변경 요청")
public record UpdateEventHidden(
        @Schema(description = "숨김 여부", example = "true")
        @NotNull
        Boolean hidden
) {
}
