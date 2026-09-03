package com.backend.comment.controller;


import com.backend.comment.dto.CreateComment;
import com.backend.comment.dto.CommentResponse;
import com.backend.comment.dto.UpdateComment;
import com.backend.comment.service.CommentService;
import com.backend.common.MsgResponse;
import com.backend.config.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 API")
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @Operation(
            summary = "댓글 생성",
            description = "댓글을 만듭니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "댓글 생성 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CommentResponse createComment(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
                                         @PathVariable Long eventId,
                                         @Valid @RequestBody CreateComment request){
        return commentService.createComment(token, eventId, request);
    }

    @Operation(
            summary = "댓글 수정",
            description = "댓글을 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 수정 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{commentId}/update")
    @PreAuthorize("isAuthenticated()")
    public CommentResponse updateComment(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
                                         @PathVariable Long commentId,
                                         @Valid @RequestBody UpdateComment request){
        return commentService.updateComment(token, commentId, request);
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "이벤트의 댓글 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/{eventId}/list")
    public List<CommentResponse> getEventCommentList(@RequestHeader(value = JwtAuthenticationFilter.TOKEN_HEADER, required = false) String token,
                                                     @PathVariable Long eventId){
        return commentService.getEventCommentList(token, eventId);
    }

    @Operation(
            summary = "자신의 댓글 목록 조회",
            description = "자신의 댓글 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/list")
    public List<CommentResponse> getMyCommentList(@RequestHeader(value = JwtAuthenticationFilter.TOKEN_HEADER, required = false) String token){
        return commentService.getMyCommentList(token);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{commentId}/delete")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse deleteComment(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
                                     @PathVariable Long commentId){
        commentService.deleteComment(token, commentId);
        return new MsgResponse("삭제에 성공했습니다.", "200");
    }

    @Operation(
            summary = "댓글 숨김",
            description = "댓글을 숨김처리합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 숨김 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{commentId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse hideComment(@PathVariable Long commentId){
        commentService.hideComment(commentId);
        return new MsgResponse("숨김에 성공했습니다.", "200");
    }

    @Operation(
            summary = "댓글 숨김 해제",
            description = "댓글을 숨김을 해제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 숨김 해제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PatchMapping("/{commentId}/unhide")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse unhideComment(@PathVariable Long commentId){
        commentService.unhideComment(commentId);
        return new MsgResponse("숨김해제에 성공했습니다.", "200");
    }
}
