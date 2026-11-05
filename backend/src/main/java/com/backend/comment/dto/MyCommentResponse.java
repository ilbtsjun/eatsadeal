package com.backend.comment.dto;

import com.backend.comment.entity.Comment;
import com.backend.comment.entity.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 댓글 정보 응답")
public record MyCommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long commentId,

        @Schema(description = "댓글이 달린 이벤트 ID", example = "1")
        Long eventId,

        @Schema(description = "댓글이 달린 이벤트 제목", example = "별 하나 페스티벌 라인업 안내")
        String eventTitle,

        @Schema(description = "댓글 내용", example = "댓글")
        String content,

        @Schema(description = "댓글 상태", example = "ACTIVE")
        CommentStatus status,

        @Schema(description = "댓글 생성 시각", example = "2026-09-01T00:00:00")
        LocalDateTime createdAt
) {
    public static MyCommentResponse from(Comment comment) {
        String content = comment.getContent();
        if (comment.getCommentStatus() == CommentStatus.HIDDEN) {
            content = "숨김 처리된 댓글입니다.";
        }
        if (comment.getCommentStatus() == CommentStatus.DELETED) {
            content = "삭제된 댓글입니다.";
        }
        return new MyCommentResponse(
                comment.getCommentId(),
                comment.getEvent().getId(),
                comment.getEvent().getTitle(),
                content,
                comment.getCommentStatus(),
                comment.getCreatedAt()
        );
    }
}
