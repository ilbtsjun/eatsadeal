package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "이벤트 수정 요청")
public record UpdateEvent(
        @Schema(description = "제목", example = "반마리 추가로 든든하게, 더하닭! 애드닭!")
        @Size(max = 500, message = "이벤트 제목은 500자 이하여야 합니다.")
        String title,

        @Schema(description = "내용" , example = "한마리 구매시 추가")
        @Size(max = 255, message = "이벤트 내용은 255자 이하여야 합니다.")
        String description,

        @Schema(description = "이벤트 url", example = "https://www.bhc.co.kr/event/currentEvent/33")
        @Size(max = 700, message = "이벤트 URL은 700자 이하여야 합니다.")
        String url,

        @Schema(description = "이벤트 이미지 url", example = "https://home-img.bhc.co.kr/bhc/event/20251112_103354_639fc48d.png")
        @Size(max = 1000, message = "이미지 URL은 1000자 이하여야 합니다.")
        String img,

        @Schema(description = "시작 시각", example = "2026-01-01T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료 시각", example = "2026-12-31T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "종료 여부", example = "false")
        Boolean isActive,

        @Schema(description = "이벤트 종류", example = "[]")
        Set<EventCode> eventCodes
) {
}
