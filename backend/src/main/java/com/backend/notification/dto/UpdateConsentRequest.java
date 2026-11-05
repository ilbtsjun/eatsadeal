package com.backend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "수신 동의 변경 요청")
public record UpdateConsentRequest(
        @Schema(description = "마케팅 활용 동의 여부", example = "true")
        @NotNull
        Boolean marketingAgreed,

        @Schema(description = "광고성 이메일 수신 동의 여부", example = "true")
        @NotNull
        Boolean emailAgreed,

        @Schema(description = "동의 경로", example = "SUBSCRIBE_POPUP")
        @NotNull
        ConsentSource source
) {
}
