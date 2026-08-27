package com.backend.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 메시지 응답")
public record MsgResponse(
        @Schema(description = "결과 메시지", example = "처리 결과")
        String msg,

        @Schema(description = "상태 코드", example = "200")
        String stat
) {
}