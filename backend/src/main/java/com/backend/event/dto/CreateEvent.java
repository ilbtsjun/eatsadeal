package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "이벤트 생성 요청")
public record CreateEvent(
        @Schema(description = "이벤트 제목", example = "bhc 멤버십 헤택, 자주 먹을수록 커지는 혜택!")
        @NotBlank(message = "이벤트 제목은 필수입니다.")
        String title,

        @Schema(description = "이벤트 내용", example = "null")
        String description,

        @Schema(description = "이벤트 URL", example = "https://www.bhc.co.kr/event/currentEvent/30")
        @NotBlank(message = "이벤트 URL은 필수입니다.")
        String url,

        @Schema(description = "이벤트 이미지 URL", example = "https://home-img.bhc.co.kr/bhc/event/20251112_101844_1d07470a.png")
        @NotBlank(message = "이미지 URL은 필수입니다.")
        String img,

        @Schema(description = "시작일", example = "2025-04-29T00:00:00")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDateTime startDate,

        @Schema(description = "종료일", example = "2026-12-31T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "브랜드 ID", example = "1")
        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,

        @Schema(description = "이벤트 코드", example = "null")
        Set<EventCode> eventCodes
) {
        @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
        public boolean isDateRangeValid() {
                if (startDate == null || endDate == null) {
                        return true;
                }

                return !endDate.isBefore(startDate);
        }
}
