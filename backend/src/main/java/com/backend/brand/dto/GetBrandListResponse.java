package com.backend.brand.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "브랜드 리스트 요청")
public record GetBrandListResponse(
        @Schema(description = "브랜드 ID", example = "1")
        Long id,

        @Schema(description = "브랜드 이름", example = "BHC")
        String name,

        @Schema(description = "브랜드 이미지 URL", example = "https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg")
        String img
) {
}