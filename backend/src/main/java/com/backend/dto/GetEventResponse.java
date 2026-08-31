package com.backend.dto;

import com.backend.entity.EventCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "이벤트 조회 요청")
public record GetEventResponse(
        @Schema(description = "이벤트 ID", example = "1")
        Long id,

        @Schema(description = "이벤트 제목", example = "별 하나 페스티벌 라인업 안내")
        String title,

        @Schema(description = "이벤트 내용", example = "null")
        String description,

        @Schema(description = "이벤트 URL", example = "https://www.bhc.co.kr/event/currentEvent/79")
        String url,

        @Schema(description = "이벤트 이미지 URL", example = "https://home-img.bhc.co.kr/bhc/event/20260409_163109_4d102ab9.png")
        String img,

        @Schema(description = "시작일", example = "2026-04-10T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "종료일", example = "2026-12-31T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "조회수", example = "10")
        Long viewCount,

        @Schema(description = "종료 여부", example = "false")
        Boolean isActive,

        @Schema(description = "브랜드 ID")
        Long brandId,

        @Schema(description = "브랜드 이름")
        String brandName,

        @Schema(description = "브랜드 이미지")
        String brandImg,


        @Schema(description = "이벤트 코드", example = "GIFT_PROMO")
        Set<EventCode> eventCodes) {

}
