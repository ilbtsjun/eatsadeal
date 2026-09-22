package com.backend.auth.controller;

import com.backend.auth.dto.*;
import com.backend.auth.service.AuthService;
import com.backend.common.dto.MsgResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "새로운 유저를 등록합니다."
    )
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@Valid @RequestBody SignUp request){
        authService.signUp(request);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 토큰을 발급합니다."
    )
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }

    @Operation(
            summary = "로그아웃",
            description = "JWT 토큰을 무효화하여 로그아웃합니다."
    )
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public MsgResponse logout(HttpServletRequest request){
        authService.logout(request);
        return new MsgResponse("로그아웃이 완료되었습니다.", "200");
    }

    @Operation(
            summary = "이메일 인증 요청",
            description = "이메일로 인증을 검증합니다."
    )
    @PostMapping("/email-verification")
    public void validateEmail(@Valid @RequestBody EmailValification request) {
        authService.validateEmail(request);
    }

    @Operation(
            summary = "비밀번호 재설정 요청",
            description = "이메일로 인증요청을 보냅니다."
    )
    @PostMapping("/password-change")
    public void passwordChange(@Valid @RequestBody PasswordChange request) {
        authService.changePassword(request);
    }

    @Operation(
            summary = "비밀번호 재설정 인증 요청",
            description = "이메일로 인증요청을 보냅니다."
    )
    @PostMapping("/password-verification")
    public void validatePassword(@Valid @RequestBody PasswordValification request) {
        authService.validatePassword(request);
    }
}
