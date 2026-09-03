package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "유저 정지 요청")
public record SuspendUser(
        @Schema(description = "정지 시간")
        @NotNull
        @Min(1)
        Long suspendTime,

        @Schema(description = "정지 이유")
        @NotBlank
        String suspendReason
) {
}
