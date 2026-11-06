package com.backend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "수신거부 요청")
public record UnsubscribeRequest(
        @Schema(description = "메일의 수신거부 링크에 담긴 토큰", example = "111111111111111111111111111111")
        @NotBlank(message = "수신거부 토큰은 필수입니다.")
        String token
) {
}
