package com.backend.user.controller;

import com.backend.common.dto.MsgResponse;
import com.backend.favorite.service.FavoriteService;
import com.backend.user.dto.*;
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
@Tag(name = "User", description = "유저 API")
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final FavoriteService favoriteService;

    @Operation(
            summary = "이메일 중복 확인",
            description = "이미 존재하는 이메일인지 확인합니다."
    )
    @GetMapping("/email-availability")
    public boolean isExistEmail(@RequestParam String emailID) {
        return userService.isExistEmail(emailID);
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "이미 존재하는 닉네임인지 확인합니다."
    )
    @GetMapping("/nickname-availability")
    public boolean isExistNickname(@RequestParam String nickname) {
        return userService.isExistNickname(nickname);
    }

    @Operation(
            summary = "마이페이지",
            description = "JWT 토큰으로 로그인한 유저의 마이페이지 정보를 조회합니다."
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public GetMyPageResponse getMyPage() {
        return userService.getMyPage();
    }

    @Operation(
            summary = "마이페이지 수정",
            description = "JWT 토큰으로 로그인한 유저의 닉네임, 전화번호, 이름, 프로필 사진을 수정합니다."
    )
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse updateMyPage(@Valid @RequestBody UpdateMyPage request) {
        userService.updateMyPage(request);
        return new MsgResponse("마이페이지 수정이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "JWT 토큰으로 로그인한 유저의 비밀번호를 변경합니다."
    )
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse updatePassword(@Valid @RequestBody UpdatePassword request) {
        userService.updatePassword(request);
        return new MsgResponse("비밀번호 수정이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "JWT 토큰으로 로그인한 유저의 탈퇴를 처리합니다."
    )
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse quit(@Valid @RequestBody QuitUser request){
        userService.quit(request);
        return new MsgResponse("탈퇴가 성공적으로 완료되었습니다.", "200");
    }

    @Operation(
            summary = "유저 정보 조회",
            description = "JWT 토큰으로 관리자 인증을 한 뒤 유저의 정보를 조회합니다."
    )
    @GetMapping("/admin/{userID}")
    @PreAuthorize("hasRole('ADMIN')")
    public GetMyPageResponse getUserInfo(@PathVariable Long userID) {
        return userService.getUserInfo(userID);
    }

    @Operation(
            summary = "회원 정지",
            description = "JWT 토큰으로 관리자 인증을 한 뒤 유저의 정지를 처리합니다."
    )
    @PatchMapping("/admin/{userID}/suspension")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse suspensionUser(@PathVariable Long userID,
                                    @Valid @RequestBody SuspensionUser request){
        userService.suspensionUser(userID, request);
        return new MsgResponse("처리가 완료되었습니다.", "200");
    }

    @Operation(
            summary = "즐겨찾기 목록을 반환합니다.",
            description = "JWT 토큰으로 로그인한 유저를 찾고, 유저의 즐겨찾기 목록을 반환합니다."
    )
    @GetMapping("/me/favorites")
    @PreAuthorize("isAuthenticated()")
    public List<Long> toggleFavorite() {
        return favoriteService.getFavoriteList();
    }
}
