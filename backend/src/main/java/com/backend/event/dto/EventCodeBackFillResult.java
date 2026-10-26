package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 코드 일괄 분류 결과")
public record EventCodeBackFillResult(
        @Schema(description = "이번에 살펴본 이벤트 수") int processed,
        @Schema(description = "코드나 설명이 채워진 이벤트 수") int classified,
        @Schema(description = "다음 호출에 넘길 afterId (더 처리할 이벤트가 없으면 null)") Long nextAfterId,
        @Schema(description = "오늘 사용한 AI 호출 수") int aiCallsToday,
        @Schema(description = "하루 AI 호출 상한") int aiDailyLimit
) {
}
