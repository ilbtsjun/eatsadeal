package com.backend.brand.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "브랜드 조회 요청")
public record GetBrandResponse(
        @Schema(description = "브랜드 ID", example = "1")
        Long id,

        @Schema(description = "브랜드 이름", example = "BHC")
        String name,

        @Schema(description = "브랜드 URL", example = "https://www.bhc.co.kr/main")
        String url,

        @Schema(description = "브랜드 이미지 URL", example = "https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg")
        String img,

        @Schema(description = "카테고리 목록", example = "[]")
        List<Long> categoryIds
) {
}
