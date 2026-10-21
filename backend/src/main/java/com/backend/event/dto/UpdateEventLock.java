package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이벤트 크롤러 잠금 변경 요청")
public record UpdateEventLock(
        @Schema(description = "잠금 여부(true면 크롤러가 덮어쓰지 않음)", example = "false")
        @NotNull
        Boolean adminLocked
) {
}
