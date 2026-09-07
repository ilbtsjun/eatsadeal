package com.backend.auth.controller;

import com.backend.auth.dto.LoginRequest;
import com.backend.auth.dto.LoginResponse;
import com.backend.auth.dto.SignUp;
import com.backend.auth.service.AuthService;
import com.backend.common.dto.MsgResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {
    private final AuthService authService;

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
    public MsgResponse signUp(@Valid @RequestBody SignUp request){
        authService.signUp(request);
        return new MsgResponse("회원가입 완료","201");
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
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
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
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public MsgResponse logout(HttpServletRequest request){
        authService.logout(request);
        return new MsgResponse("로그아웃이 완료되었습니다.", "200");
    }

//    @Operation(
//            summary = "이메일 인증 요청",
//            description = "이메일로 인증요청을 보냅니다.",
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "송신 성공"
//                    ),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "토큰이 없거나 유효하지 않음"
//                    ),
//                    @ApiResponse(
//                            responseCode = "401",
//                            description = "인증 실패"
//                    )
//            }
//    )
//    @PostMapping("/email-verification")
//    public void sendCode(@Valid @RequestBody SendEmailVerificationRequest request) {
//        authService.sendCode(request.email());
//    }
//
//    @PostMapping("/email-confirm")
//    public void verifyCode(@Valid @RequestBody VerifyEmailRequest request) {
//        authService.verifyCode(request.email(), request.code());
//    }
}
