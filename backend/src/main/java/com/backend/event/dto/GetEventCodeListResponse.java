package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 코드 목록 조회 요청")
public record GetEventCodeListResponse(
        @Schema(description = "이벤트 코드", example = "1")
        String eventCode,

        @Schema(description = "이름", example = "DISCOUNT_PRICE")
        String title,

        @Schema(description = "설명", example = "특정 금액 즉시 할인")
        String description
) {
    public static GetEventCodeListResponse from(EventCode eventCode) {
        return new GetEventCodeListResponse(
                eventCode.name(),
                eventCode.getTitle(),
                eventCode.getDescription()
        );
    }
}
