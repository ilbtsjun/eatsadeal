package com.backend.dto;

import com.backend.common.EventCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "검색 요청")
public record GetSearch(
        @Schema(description = "브랜드 ID", example = "1")
        Long brandId,

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "이벤트 코드", example = "GIFT_PROMO")
        EventCode eventCode,

        @Schema(description = "검색어", example = "하나")
        String keyword,

        @Schema(description = "정렬 방법", example = "popular")
        String sort,

        @Schema(description = "페이지 번호", example = "1")
        int page,

        @Schema(description = "페이지당 개수", example = "20")
        int size
) {
}
