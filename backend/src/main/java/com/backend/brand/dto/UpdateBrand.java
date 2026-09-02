package com.backend.brand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "브랜드 수정 요청")
public record UpdateBrand(
        @Schema(description = "브랜드 이름", example = "BHC")
        @Size(max = 50, message = "브랜드 이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "브랜드 URL", example = "https://www.bhc.co.kr/main")
        String url,

        @Schema(description = "브랜드 이미지 URL", example = "https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg")
        String img,

        @Schema(description = "카테고리 목록", example = "[]")
        List<Long> categoryIds
) {
}
