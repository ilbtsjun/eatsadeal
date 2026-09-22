package com.backend.comment.dto;

import com.backend.comment.entity.Comment;
import com.backend.comment.entity.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "댓글 정보 요청")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long commentId,

        @Schema(description = "댓글이 달린 이벤트 ID", example = "1")
        Long eventId,

        @Schema(description = "댓글을 작성한 유저 ID", example = "1")
        Long userId,

        @Schema(description = "유저 닉네임", example = "김삿갓")
        String nickname,

        @Schema(description = "댓글 내용", example = "댓글")
        String content,

        @Schema(description = "댓글 상태", example = "ACTIVE")
        CommentStatus status,

        @Schema(description = "댓글 생성 시각", example = "2026-09-01T00:00:00")
        LocalDateTime createdAt,

        @Schema(description = "댓글 수정 시각", example = "2026-09-01T09:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "자신이 쓴 댓글 확인", example = "true")
        boolean isMine
) {
    public static CommentResponse from(Comment comment, Long userId) {
        String content = comment.getContent();
        if(comment.getCommentStatus() == CommentStatus.HIDDEN){
            content = "숨김 처리된 댓글입니다.";
        }
        if(comment.getCommentStatus() == CommentStatus.DELETED){
            content = "삭제된 댓글입니다.";
        }
        boolean isMine = userId == null
                ? false
                : comment.getUser().getId().equals(userId);
        return new CommentResponse(
                comment.getCommentId(),
                comment.getEvent().getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                content,
                comment.getCommentStatus(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isMine
        );
    }
}
