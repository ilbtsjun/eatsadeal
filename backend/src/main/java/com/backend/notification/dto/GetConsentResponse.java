package com.backend.notification.dto;

import com.backend.notification.entity.NotificationSetting;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "수신 동의 조회 응답")
public record GetConsentResponse(
        @Schema(description = "마케팅 활용 동의 여부", example = "true")
        Boolean marketingAgreed,

        @Schema(description = "마케팅 활용 동의 일시", example = "2026-11-04T10:00:00")
        LocalDateTime marketingAgreedAt,

        @Schema(description = "광고성 이메일 수신 동의 여부", example = "true")
        Boolean emailAgreed,

        @Schema(description = "광고성 이메일 수신 동의 일시", example = "2026-11-04T10:00:00")
        LocalDateTime emailAgreedAt
) {
    public static GetConsentResponse from(NotificationSetting setting) {
        return new GetConsentResponse(
                setting.getMarketingAgreed(),
                setting.getMarketingAgreedAt(),
                setting.getEmailAgreed(),
                setting.getEmailAgreedAt());
    }

    public static GetConsentResponse empty() {
        return new GetConsentResponse(false, null, false, null);
    }
}
