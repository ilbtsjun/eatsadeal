package com.backend.user.controller;

import com.backend.common.MsgResponse;
import com.backend.config.JwtAuthenticationFilter;
import com.backend.user.dto.*;
import com.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 API")
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "새로운 유저를 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "회원가입 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 중복 데이터"
                    )
            }
    )
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MsgResponse signUp(@Valid @RequestBody CreateUser request){
        userService.singUp(request);
        return new MsgResponse("회원가입 완료","201");
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "이미 존재하는 이메일인지 확인합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값이 유효하지 않음"
                    )
            }
    )
    @GetMapping("/email/{emailID}")
    public boolean isExistEmail(@PathVariable String emailID) {
        return userService.isExistEmail(emailID);
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "이미 존재하는 닉네임인지 확인합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값이 유효하지 않음"
                    )
            }
    )
    @GetMapping("/nickname/{nickname}")
    public boolean isExistNickname(@PathVariable String nickname) {
        return userService.isExistNickname(nickname);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 토큰을 발급합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패 또는 인증 실패"
                    )
            }
    )
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return userService.login(request);
    }

    @Operation(
            summary = "로그아웃",
            description = "JWT 토큰을 무효화하여 로그아웃합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그아웃 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PostMapping("/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse logout(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token){
        userService.logout(token);
        return new MsgResponse("로그아웃이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "마이페이지",
            description = "JWT 토큰으로 로그인한 유저의 마이페이지 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/mypage")
    @PreAuthorize("isAuthenticated()")
    public GetMyPageResponse getMyPage(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token) {
        return userService.getMyPage(token);
    }

    @Operation(
            summary = "마이페이지 수정",
            description = "JWT 토큰으로 로그인한 유저의 닉네임, 전화번호, 이름, 프로필 사진을 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PutMapping("/mypage/update")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse updateMyPage(
            @RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
            @Valid @RequestBody UpdateMyPage request) {
        userService.updateMyPage(token, request);
        return new MsgResponse("마이페이지 수정이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "JWT 토큰으로 로그인한 유저의 비밀번호를 변경합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "변경 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PutMapping("/mypage/updatePassword")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse updatePassword(
            @RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token,
            @Valid @RequestBody UpdatePassword request) {
        userService.updatePassword(token, request);
        return new MsgResponse("비밀번호 수정이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "JWT 토큰으로 로그인한 유저의 탈퇴를 처리합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "탈퇴 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @DeleteMapping("/quit")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse quit(@RequestHeader(JwtAuthenticationFilter.TOKEN_HEADER) String token
                            ,@Valid @RequestBody QuitUser request){
        userService.quitUser(token, request);
        return new MsgResponse("탈퇴가 성공적으로 완료되었습니다.", "200");
    }

    @Operation(
            summary = "유저 정보 조회",
            description = "JWT 토큰으로 관리자 인증을 한 뒤 유저의 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/admin/nickname/{nickname}")
    @PreAuthorize("hasRole('ADMIN')")
    public GetMyPageResponse getUserInfoByNickname(@PathVariable String nickname) {
        return userService.getUserInfoByNickname(nickname);
    }

    @GetMapping("/{userID}")
    @PreAuthorize("hasRole('ADMIN')")
    public GetMyPageResponse getUserInfo(@PathVariable Long userID) {
        return userService.getUserInfo(userID);
    }

    @Operation(
            summary = "회원 정지",
            description = "JWT 토큰으로 관리자 인증을 한 뒤 유저의 정지를 처리합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "정지 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PutMapping("/{userID}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse suspendUser(@PathVariable Long userID,
                                    @Valid @RequestBody SuspendUser request){
        userService.suspendUser(userID, request);
        return new MsgResponse("정지가 완료되었습니다.", "200");
    }

    @Operation(
            summary = "회원 정지 해제",
            description = "JWT 토큰으로 관리자 인증을 한 뒤 유저의 정지를 해제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "정지 해제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰이 없거나 유효하지 않음, 입력값 검증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @PutMapping("/{userID}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public MsgResponse suspendUser(@PathVariable Long userID){
        userService.activeUser(userID);
        return new MsgResponse("정지가 해제되었습니다.", "200");
    }
}
