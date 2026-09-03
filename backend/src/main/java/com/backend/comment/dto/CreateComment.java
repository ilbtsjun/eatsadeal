package com.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 생성 요청")
public record CreateComment(
        @Schema(description = "댓글 내용", example = "댓글")
        @NotBlank
        String content
) {
}
