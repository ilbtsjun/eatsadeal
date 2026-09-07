package com.backend.user.service;

import com.backend.auth.service.AuthService;
import com.backend.auth.service.CurrentUserService;
import com.backend.user.dto.*;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public boolean isExistEmail(String email){
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isExistNickname(String nickname){
        return userRepository.existsByNickname(nickname);
    }

    @Transactional
    public GetMyPageResponse getMyPage() {
        User user = currentUserService.getRequiredUser();
        return GetMyPageResponse.from(user);
    }

    @Transactional
    public void updateMyPage(UpdateMyPage request) {
        User user = currentUserService.getRequiredUser();

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
        if (!newUserNickname.equals(user.getNickname()) && userRepository.existsByNickname(newUserNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.update(newName, newUserNickname, newPhoneNumber, newBirth);
    }

    @Transactional
    public void updatePassword(UpdatePassword request) {
        User user = currentUserService.getRequiredUser();

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

//    @Transactional
//    public void quit(QuitUser request){
//        User user = currentUserService.getRequiredUser();
//        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
//            throw new IllegalArgumentException("비밀번호가 다릅니다.");
//        }
//
//        user.withdrawn();
//        tokenBlacklist.add(token, jwtTokenProvider.getExpiration(token));
//    }
}
