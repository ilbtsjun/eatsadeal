package com.backend.admin.controller;

import com.backend.admin.dto.AdminDashboardResponse;
import com.backend.admin.service.AdminDashboardService;
import com.backend.comment.dto.CommentResponse;
import com.backend.comment.service.CommentService;
import com.backend.common.dto.MsgResponse;
import com.backend.user.dto.GetMyPageResponse;
import com.backend.user.dto.GetUserInfoRequest;
import com.backend.user.dto.SuspensionUser;
import com.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "관리자 대시보드와 회원·댓글 관리 API")
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminDashboardService adminDashboardService;
    private final UserService userService;
    private final CommentService commentService;

    @Operation(
            summary = "관리자 대시보드",
            description = "이벤트, 크롤링, AI 사용량, 회원·댓글 지표를 한 번에 반환합니다."
    )
    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @Operation(
            summary = "회원 정보 조회",
            description = "아이디나 닉네임으로 회원 정보를 조회합니다."
    )
    @GetMapping("/users")
    public GetMyPageResponse getUserInfo(@ModelAttribute GetUserInfoRequest request) {
        return userService.getUserInfo(request);
    }

    @Operation(
            summary = "회원 정지·해제",
            description = "회원을 정지하거나 정지를 해제합니다."
    )
    @PatchMapping("/users/{userId}/suspension")
    public MsgResponse suspensionUser(@PathVariable Long userId,
                                      @Valid @RequestBody SuspensionUser request) {
        userService.suspensionUser(userId, request);
        return new MsgResponse("처리가 완료되었습니다.", "200");
    }

    @Operation(
            summary = "회원 댓글 목록 조회",
            description = "특정 회원이 작성한 댓글 목록을 조회합니다."
    )
    @GetMapping("/users/{userId}/comments")
    public List<CommentResponse> getUserCommentList(@PathVariable Long userId) {
        return commentService.getUserCommentList(userId);
    }

    @Operation(
            summary = "댓글 숨김",
            description = "댓글을 숨김 처리합니다."
    )
    @PatchMapping("/comments/{commentId}/hide")
    public MsgResponse hideComment(@PathVariable Long commentId) {
        commentService.hideComment(commentId);
        return new MsgResponse("숨김에 성공했습니다.", "200");
    }

    @Operation(
            summary = "댓글 숨김 해제",
            description = "댓글의 숨김을 해제합니다."
    )
    @PatchMapping("/comments/{commentId}/unhide")
    public MsgResponse unhideComment(@PathVariable Long commentId) {
        commentService.unhideComment(commentId);
        return new MsgResponse("숨김해제에 성공했습니다.", "200");
    }
}
