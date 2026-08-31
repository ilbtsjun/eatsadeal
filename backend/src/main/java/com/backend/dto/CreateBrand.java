package com.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "브랜드 생성 요청")
public record CreateBrand(
        @Schema(description = "브랜드 이름", example = "BHC")
        @NotBlank(message = "브랜드 이름은 필수입니다.")
        @Size(max = 50, message = "브랜드 이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "브랜드 URL", example = "https://www.bhc.co.kr")
        @NotBlank(message = "브랜드 URL은 필수입니다.")
        String url,

        @Schema(description = "브랜드 이미지 URL", example = "https://www.bhc.co.kr/_next/static/media/ico_logo_footer.643042c8.svg")
        @NotBlank(message = "브랜드 이미지 URL은 필수입니다.")
        String img,

        @Schema(description = "카테고리 목록", example = "[]")
        @NotNull(message = "카테고리 목록은 null일 수 없습니다.")
        @NotEmpty(message = "카테고리는 하나 이상 선택해야 합니다.")
        List<Long> categoryIds
) {
}
