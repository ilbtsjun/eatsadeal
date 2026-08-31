package com.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateCategory(
        @Schema(description = "수정할 이미지 주소", example = "https://www.magnific.com/kr/free-psd/crispy-fried-chicken-drumsticks-plate_409843237.htm#fromView=keyword&page=1&position=0&uuid=2725dbc8-caf0-4638-86a8-ddc379f9ae01&track=ais_hybrid&query=%EC%B9%98%ED%82%A8")
        String img
) {
}
