package com.backend.auth.service;

import com.backend.auth.dto.*;
import com.backend.auth.resolver.BearerTokenResolver;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisJsonStore;
import com.backend.common.redis.RedisKeyUtil;
import com.backend.mail.service.MailService;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final MailService mailService;
    private final RedisJsonStore redisJsonStore;
    private final MailVerificationPolicy mailVerificationPolicy;


    private static final String PENDING_PREFIX = "pending:user:";

    private static final String PASSWORD_PREFIX = "password:change:";

    private static final String SIGNUP_PURPOSE = "SIGNUP";

    private static final String PASSWORD_RESET_PURPOSE = "PASSWORD_RESET";


    @Transactional
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

    public void signUp(SignUp request) {
        String email = RedisKeyUtil.normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if(userRepository.existsByEmail(email) || userRepository.existsByNickname(nickname)){
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        }

        mailVerificationPolicy.validateSendingAllowed(SIGNUP_PURPOSE, email);

        String authCode = createCode();

        SignUp pendingUser = new SignUp(
                request.name(),
                email,
                passwordEncoder.encode(request.password()),
                nickname,
                request.phoneNumber(),
                request.userGender(),
                request.birth(),
                authCode
        );

        String key = PENDING_PREFIX + RedisKeyUtil.emailHash(email);

        redisJsonStore.save(
                key,
                pendingUser,
                Duration.ofMinutes(10)
        );

        mailService.sendSignUpMessage(email, authCode);
    }

    @Transactional
    public void validateEmail(EmailValification request) {
        String email = RedisKeyUtil.normalizeEmail(request.email());

        boolean attemptAllowed = mailVerificationPolicy.isVerificationAttemptAllowed(SIGNUP_PURPOSE, email);

        String key = PENDING_PREFIX + RedisKeyUtil.emailHash(email);

        if (!attemptAllowed) {
            redisJsonStore.delete(key);

            mailVerificationPolicy.clearAttempts(SIGNUP_PURPOSE, email);

            throw new BusinessException(ErrorCode.AUTH_FAILED, "인증번호 입력 횟수를 초과했습니다. 다시 회원가입을 진행해주세요.");
        }

        SignUp pendingUser = redisJsonStore.find(key, SignUp.class);

        if (pendingUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "인증 시간이 만료되었거나 요청 정보가 존재하지 않습니다.");
        }

        if (!pendingUser.authCode().equals(request.inputCode())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "인증번호가 일치하지 않습니다.");
        }

        User user = User.builder()
                .name(pendingUser.name().trim())
                .email(pendingUser.email().trim())
                .password(pendingUser.password())
                .nickname(pendingUser.nickname().trim())
                .phoneNumber(pendingUser.phoneNumber())
                .gender(pendingUser.userGender())
                .birth(pendingUser.birth())
                .build();

        userRepository.save(user);

        redisJsonStore.delete(key);

        mailVerificationPolicy.clearAttempts(SIGNUP_PURPOSE, email);
    }

    public void changePassword(PasswordChange request) {
        String email = RedisKeyUtil.normalizeEmail(request.email());

        mailVerificationPolicy.validateSendingAllowed(PASSWORD_RESET_PURPOSE, email);

        boolean userExists = userRepository.existsByEmail(email);

        if (!userExists) {
            return;
        }

        String authCode = createCode();

        PasswordChange pendingPassword = new PasswordChange(email, authCode);

        String key = PASSWORD_PREFIX + RedisKeyUtil.emailHash(email);

        redisJsonStore.save(
                key,
                pendingPassword,
                Duration.ofMinutes(10)
        );

        mailService.sendPasswordMessage(email, authCode);
    }

    @Transactional
    public void validatePassword(PasswordValification request) {
        if(!request.changePassword().equals(request.passwordConfirm())){
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호와 비밀번호 확인값이 다릅니다.");
        }

        String email = RedisKeyUtil.normalizeEmail(request.email());

        boolean attemptAllowed = mailVerificationPolicy.isVerificationAttemptAllowed(PASSWORD_RESET_PURPOSE, email);

        String key = PASSWORD_PREFIX + RedisKeyUtil.emailHash(email);

        if (!attemptAllowed) {
            redisJsonStore.delete(key);

            mailVerificationPolicy.clearAttempts(PASSWORD_RESET_PURPOSE, email);

            throw new BusinessException(ErrorCode.AUTH_FAILED, "인증번호 입력 횟수를 초과했습니다. 다시 요청해주세요.");
        }

        PasswordChange pendingPassword = redisJsonStore.find(key, PasswordChange.class);

        if (pendingPassword == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "인증 시간이 만료되었거나 요청 정보가 존재하지 않습니다.");
        }

        if (!pendingPassword.authCode().equals(request.inputCode())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED, "인증번호가 일치하지 않습니다.");
        }

        User user = userRepository.findByEmailOrNickname(request.email())
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FAILED, "인증번호가 만료되었거나 유효하지 않습니다."));

        user.updatePassword(passwordEncoder.encode(request.changePassword()));

        redisJsonStore.delete(key);

        mailVerificationPolicy.clearAttempts(PASSWORD_RESET_PURPOSE, email);
    }

    @Transactional
    public void addBlackList(String token, Date expiration){
        tokenBlacklist.add(token, expiration);
    }

    public String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(2);

            switch (index) {
                case 0 -> key.append((char) (random.nextInt(26) + 65)); // 대문자
                case 1 -> key.append(random.nextInt(10)); // 숫자
            }
        }
        return key.toString();
    }
}
