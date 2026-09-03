package com.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 수정 요청")
public record UpdateComment(
        @Schema(description = "수정할 댓글 내용", example = "수정된 댓글")
        @NotBlank
        String content
) {
}
