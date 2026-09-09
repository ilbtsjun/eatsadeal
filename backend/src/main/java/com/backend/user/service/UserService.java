package com.backend.user.service;

import com.backend.auth.dto.SignUp;
import com.backend.auth.service.AuthService;
import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
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
    private final AuthService authService;

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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "전화번호 형식이 올바르지 않습니다.");
        }
        if (!newUserNickname.equals(user.getNickname()) && userRepository.existsByNickname(newUserNickname)) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "이미 사용 중인 닉네임입니다.");
        }

        user.update(newName, newUserNickname, newPhoneNumber, newBirth);
    }

    @Transactional
    public void updatePassword(UpdatePassword request) {
        User user = currentUserService.getRequiredUser();

        if(!StringUtils.hasText(request.currentPassword())
                || !StringUtils.hasText(request.updatePassword())
                || !StringUtils.hasText(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호 변경 시 현재 비밀번호, 변경할 비밀번호, 변경할 비밀번호 확인이 모두 필요합니다.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCHED);
        }
        if(!request.updatePassword().equals(request.passwordConfirm())){
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCHED);
        }
        if (passwordEncoder.matches(request.updatePassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.updatePassword()));
    }

    @Transactional
    public void quit(QuitUser request){
        User user = currentUserService.getRequiredUser();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCHED);
        }

        authService.addBlackList(currentUserService.getTokenByUser(), currentUserService.getExpirationByUser());

        user.withdrawn();
    }

    @Transactional(readOnly = true)
    public GetMyPageResponse getUserInfo(Long userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return GetMyPageResponse.from(user);
    }

    @Transactional
    public void suspensionUser(Long userID, SuspensionUser request) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if(request.status()){
            if(user.getUserStatus() != UserStatus.ACTIVE){
                throw new BusinessException(ErrorCode.INVALID_STATUS);
            }
            user.suspend(request.suspendTime(), request.suspendReason());
        }
        else{
            if(user.getUserStatus() != UserStatus.SUSPEND){
                throw new BusinessException(ErrorCode.INVALID_STATUS);
            }
            user.active(request.suspendReason());
        }
    }
}
