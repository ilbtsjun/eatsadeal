package com.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 생성 요청")
public record CreateComment(
        @Schema(description = "댓글 내용", example = "댓글")
        @NotBlank
        @Size(max = 500)
        String content
) {
}
