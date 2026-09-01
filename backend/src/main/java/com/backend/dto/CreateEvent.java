package com.backend.dto;

import com.backend.common.EventCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "이벤트 생성 요청")
public record CreateEvent(
        @Schema(description = "이벤트 제목", example = "bhc 멤버십 헤택, 자주 먹을수록 커지는 혜택!")
        String title,

        @Schema(description = "이벤트 내용", example = "null")
        String description,

        @Schema(description = "이벤트 URL", example = "https://www.bhc.co.kr/event/currentEvent/30")
        String url,

        @Schema(description = "이벤트 이미지 URL", example = "https://home-img.bhc.co.kr/bhc/event/20251112_101844_1d07470a.png")
        String img,

        @Schema(description = "시작일", example = "2025-04-29T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료일", example = "2026-12-31T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "브랜드 이름", example = "BHC")
        String brandName,

        @Schema(description = "이벤트 코드", example = "null")
        Set<EventCode> eventCodes
) {
}
