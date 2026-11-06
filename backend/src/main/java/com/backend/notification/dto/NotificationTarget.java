package com.backend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 메일 발송 대상")
public record NotificationTarget(
        @Schema(description = "회원 ID", example = "1")
        Long userId,

        @Schema(description = "회원 이메일", example = "user_temp@gmail.com")
        String email,

        @Schema(description = "회원 닉네임", example = "김삿갓")
        String nickname,

        @Schema(description = "구독 브랜드 ID", example = "1")
        Long brandId
) {
}
