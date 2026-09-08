package com.backend.comment.controller;


import com.backend.comment.dto.CommentResponse;
import com.backend.comment.dto.UpdateComment;
import com.backend.comment.service.CommentService;
import com.backend.common.dto.MsgResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 API")
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    @Operation(
            summary = "자신의 댓글 목록 조회",
            description = "자신의 댓글 목록을 조회합니다."
    )
    @GetMapping
    public List<CommentResponse> getMyCommentList(){
        return commentService.getMyCommentList();
    }

    @Operation(
            summary = "댓글 수정",
            description = "댓글을 수정합니다."
    )
    @PatchMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public CommentResponse updateComment(@PathVariable Long commentId,
                                         @Valid @RequestBody UpdateComment request){
        return commentService.updateComment(commentId, request);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다."
    )
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse deleteComment(@PathVariable Long commentId){
        commentService.deleteComment(commentId);
        return new MsgResponse("삭제에 성공했습니다.", "200");
    }

    @Operation(
            summary = "댓글 숨김",
            description = "댓글을 숨김처리합니다."
    )
    @PatchMapping("/{commentId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse hideComment(@PathVariable Long commentId){
        commentService.hideComment(commentId);
        return new MsgResponse("숨김에 성공했습니다.", "200");
    }

    @Operation(
            summary = "댓글 숨김 해제",
            description = "댓글을 숨김을 해제합니다."
    )
    @PatchMapping("/{commentId}/unhide")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse unhideComment(@PathVariable Long commentId){
        commentService.unhideComment(commentId);
        return new MsgResponse("숨김해제에 성공했습니다.", "200");
    }
}
