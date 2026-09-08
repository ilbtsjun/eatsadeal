package com.backend.auth.service;

import com.backend.auth.dto.LoginRequest;
import com.backend.auth.dto.LoginResponse;
import com.backend.auth.dto.SignUp;
import com.backend.auth.resolver.BearerTokenResolver;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.mail.service.MailService;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrNickname(request.id().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        user.releaseIfExpired(LocalDateTime.now());

        String message = "정상적으로 로그인 되었습니다.";

        switch(user.getUserStatus()){
            case SUSPEND :
                message = user.getSuspendedUntil() + "까지 이용이 제한되었습니다.\n" +
                        "사유 : " +user.getSuspendingReason();
                return new LoginResponse(message,"403",null);
            case WITHDRAWN :
                message = "이미 탈퇴한 사용자입니다.";
                return new LoginResponse(message,"403",null);
        }

        user.login();

        String accessToken = jwtTokenProvider.createToken(user);

        return new LoginResponse(
                message,
                "200",
                accessToken
        );
    }

    public void logout(HttpServletRequest request) {
        String token = BearerTokenResolver.resolve(request);

        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Date expiration = jwtTokenProvider.getExpiration(token);

        addBlackList(token, expiration);
    }

    @Transactional
    public void signUp(SignUp request) {
        if(userRepository.existsByEmail(request.email()) || userRepository.existsByNickname(request.nickname())){
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }

        mailService.sendSignUpMessage(request.email());
    }

    @Transactional
    public void addBlackList(String token, Date expiration){
        tokenBlacklist.add(token, expiration);
    }

//    @Transactional
//    public void validate(SignUp request) {
//        if(userRepository.existsByEmail(request.email())){
//            throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
//        }
//        if(userRepository.existsByNickname(request.nickname())){
//            throw new IllegalArgumentException("이미 사용중인 닉네임 입니다.");
//        }
//
//        User user = User.builder()
//                .name(request.name().trim())
//                .email(request.email().trim())
//                .password(passwordEncoder.encode(request.password()))
//                .nickname(request.nickname().trim())
//                .phoneNumber(request.phoneNumber())
//                .gender(request.userGender())
//                .birth(request.birth())
//                .build();
//        userRepository.save(user);
//
//        mailService.sendSignUpMessage(request.email());
//    }
}
