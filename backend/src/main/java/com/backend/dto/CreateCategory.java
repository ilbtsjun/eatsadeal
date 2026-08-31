package com.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 생성 요청")
public record CreateCategory(
        @Schema(description = "카테고리 이름", example = "치킨")
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Size(max = 30, message = "카테고리 이름은 30자 이하여야 합니다.")
        String name,

        @Schema(description = "카테고리 이미지", example = "https://www.magnific.com/kr/free-psd/crispy-fried-chicken-drumsticks-plate_409843237.htm#fromView=keyword&page=1&position=0&uuid=2725dbc8-caf0-4638-86a8-ddc379f9ae01&track=ais_hybrid&query=%EC%B9%98%ED%82%A8")
        @NotBlank(message = "카테고리 이미지 URL은 필수입니다.")
        String img
) {
}
