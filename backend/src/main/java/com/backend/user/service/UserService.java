package com.backend.user.service;

import com.backend.config.JwtTokenProvider;
import com.backend.config.TokenBlacklist;
import com.backend.user.dto.*;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public void singUp(CreateUser request){
        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
        }
        if(userRepository.existsByNickName(request.nickName())){
            throw new IllegalArgumentException("이미 사용중인 닉네임 입니다.");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickName().trim())
                .phoneNumber(request.phoneNumber())
                .gender(request.userGender())
                .birth(request.birth())
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isExistEmail(String email){
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isExistNickname(String nickname){
        return userRepository.existsByNickName(nickname);
    }

    @Transactional
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            String message = "탈퇴한 사용자입니다.";
            return new LoginResponse(message,"403",null);
        }

        user.releaseIfExpired(LocalDateTime.now());

        if (user.hasActiveSuspension(LocalDateTime.now())) {
            String message = user.getSuspendedUntil() + "까지 이용이 제한되었습니다.\n" +
                    "사유 : " +user.getSuspendingReason();
            return new LoginResponse(message,"403",null);
        }

        String token = jwtTokenProvider.createToken(user.getId());
        user.login();
        return new LoginResponse("로그인 성공했습니다.","200",token);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (tokenBlacklist.contains(token)) {
            throw new IllegalArgumentException("이미 로그아웃된 토큰입니다.");
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
            tokenBlacklist.add(token, jwtTokenProvider.getExpiration(token));
        } catch (JwtException ex) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    @Transactional
    public GetMyPageResponse getMyPage(String token) {
        User user = findUserByToken(token);
        return GetMyPageResponse.from(user);
    }

    @Transactional
    public void updateMyPage(String token, UpdateMyPage request) {
        User user = findUserByToken(token);

        String newName = StringUtils.hasText(request.name())
                ? request.name()
                : user.getName();
        String newUserNickname = StringUtils.hasText(request.nickname())
                ? request.nickname()
                : user.getNickname();
        String newPhoneNumber = StringUtils.hasText(request.phoneNumber())
                ? request.phoneNumber()
                : user.getPhoneNumber();
        LocalDate newBirth = request.birth() == null
                ? user.getBirth()
                : request.birth();

        if(!Pattern.matches("^01[016789]-?\\d{3,4}-?\\d{4}$", newPhoneNumber)){
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }
        if (!newUserNickname.equals(user.getNickname()) && userRepository.existsByNickName(newUserNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.update(newName, newUserNickname, newPhoneNumber, newBirth);
    }

    @Transactional
    public void updatePassword(String token, UpdatePassword request) {
        User user = findUserByToken(token);

        if(!StringUtils.hasText(request.currentPassword())
                || !StringUtils.hasText(request.updatePassword())
                || !StringUtils.hasText(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호 변경 시 현재 비밀번호, 변경할 비밀번호, 변경할 비밀번호 확인이 모두 필요합니다.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if(!request.updatePassword().equals(request.passwordConfirm())){
            throw new IllegalArgumentException("변경할 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.updatePassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.updatePassword()));
    }

    @Transactional
    public void quitUser(String token, QuitUser request){
        User user = findUserByToken(token);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 다릅니다.");
        }

        user.withdrawn();
        tokenBlacklist.add(token, jwtTokenProvider.getExpiration(token));
    }

    @Transactional(readOnly = true)
    public GetMyPageResponse getUserInfo(Long userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        return GetMyPageResponse.from(user);
    }

    @Transactional
    public void suspendUser(Long userID, SuspendUser request) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        user.suspend(request.suspendTime(), request.suspendReason());
    }

    @Transactional
    public void activeUser(Long userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        if(user.getUserStatus() != UserStatus.SUSPEND){
            throw new IllegalArgumentException("현재 정지상태가 아닙니다.");
        }
        user.releaseSuspend();
    }

    private User findUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (tokenBlacklist.contains(token)) {
            throw new IllegalArgumentException("이미 로그아웃된 토큰입니다.");
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
            Long userId = Long.valueOf(jwtTokenProvider.getSubject(token));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            user.releaseIfExpired(LocalDateTime.now());
            if(user.getUserStatus().equals(UserStatus.ACTIVE)){
                return user;
            }
            throw new IllegalArgumentException("정지되거나 탈퇴한 사용자입니다.");
        } catch (JwtException | NumberFormatException ex) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }
}
