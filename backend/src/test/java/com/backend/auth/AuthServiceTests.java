package com.backend.auth;

import com.backend.auth.dto.*;
import com.backend.auth.resolver.BearerTokenResolver;
import com.backend.auth.service.AuthService;
import com.backend.auth.service.MailVerificationPolicy;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisJsonStore;
import com.backend.mail.service.MailService;
import com.backend.user.dto.UserGender;
import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private MailService mailService;

    @Mock
    private RedisJsonStore redisJsonStore;

    @Mock
    private MailVerificationPolicy mailVerificationPolicy;

    @InjectMocks
    private AuthService authService;

    private User buildUser(String email, String nickname, String encodedPassword) {
        return User.builder()
                .name("홍길동")
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .phoneNumber("010-1111-2222")
                .gender(UserGender.values()[0])
                .birth(LocalDate.of(1990, 1, 1))
                .build();
    }

    private LoginRequest loginRequest(String id, String password) {
        return new LoginRequest(id, password);
    }

    private SignUp signUpRequest(String name, String email, String password, String nickname,
                                 String phoneNumber, LocalDate birth) {
        return new SignUp(name, email, password, nickname, phoneNumber, UserGender.values()[0], birth, "UNUSED");
    }

    private EmailValification emailValidationRequest(String email, String inputCode) {
        return new EmailValification(email, inputCode);
    }

    private PasswordValification passwordValidationRequest(String email, String inputCode,
                                                           String changePassword, String confirm) {
        return new PasswordValification(email, inputCode, changePassword, confirm);
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("성공: ACTIVE 유저가 비밀번호 일치 시 토큰을 발급받고 200 응답을 받는다")
        void success() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("raw-password", "encoded")).thenReturn(true);
            when(jwtTokenProvider.createToken(user)).thenReturn("issued-token");

            LoginResponse response = authService.login(loginRequest("user@test.com", "raw-password"));

            assertEquals("200", response.status());
            assertEquals("issued-token", response.token());
            assertNotNull(user.getLastLoginAt(), "로그인 시각이 기록되어야 한다");
        }

        @Test
        @DisplayName("성공: id 앞뒤 공백은 제거되어 조회된다")
        void trimsId() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.createToken(user)).thenReturn("token");

            authService.login(loginRequest("  user@test.com  ", "raw-password"));

            verify(userRepository).findByEmailOrNickname("user@test.com");
        }

        @Test
        @DisplayName("실패: 아이디(이메일/닉네임)가 없으면 LOGIN_FAILED, 비밀번호 비교는 하지 않는다")
        void userNotFound() {
            when(userRepository.findByEmailOrNickname("nobody")).thenReturn(Optional.empty());

            assertBusinessException(() -> authService.login(loginRequest("nobody", "raw-password")), ErrorCode.LOGIN_FAILED);

            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        @DisplayName("실패: 비밀번호가 틀리면 LOGIN_FAILED, 토큰을 발급하지 않는다")
        void wrongPassword() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            assertBusinessException(() -> authService.login(loginRequest("user@test.com", "wrong")), ErrorCode.LOGIN_FAILED);

            verifyNoInteractions(jwtTokenProvider);
            assertNull(user.getLastLoginAt());
        }

        @Test
        @DisplayName("정지 중(만료 전): 예외 없이 403 응답과 정지 사유가 담긴 메시지를 반환한다. 토큰은 발급하지 않는다")
        void suspendedUser() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            user.suspend(30L, "규정 위반");
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));

            LoginResponse response = authService.login(loginRequest("user@test.com", "raw-password"));

            assertEquals("403", response.status());
            assertNull(response.token());
            assertTrue(response.msg().contains("규정 위반"));
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("정지 만료됨: releaseIfExpired로 ACTIVE가 되어 정상 로그인된다")
        void suspensionExpired() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            user.suspend(30L, "규정 위반");
            ReflectionTestUtils.setField(user, "suspendedUntil", LocalDateTime.now().minusDays(1));
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.createToken(user)).thenReturn("token");

            LoginResponse response = authService.login(loginRequest("user@test.com", "raw-password"));

            assertEquals("200", response.status());
            assertEquals(UserStatus.ACTIVE, user.getUserStatus());
        }

        @Test
        @DisplayName("탈퇴한 유저: 예외 없이 403과 탈퇴 안내 메시지를 반환한다. 토큰은 발급하지 않는다")
        void withdrawnUser() {
            User user = buildUser("user@test.com", "닉네임", "encoded");
            user.withdrawn();
            when(userRepository.findByEmailOrNickname(anyString())).thenReturn(Optional.of(user));
            lenient().when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            LoginResponse response = authService.login(loginRequest("user@test.com", "raw-password"));

            assertEquals("403", response.status());
            assertNull(response.token());
            assertTrue(response.msg().contains("탈퇴"));
            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("성공: 요청에서 꺼낸 토큰의 만료 시각을 조회해 블랙리스트에 등록한다")
        void success() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Date expiration = new Date();
            when(jwtTokenProvider.getExpiration("jwt-token")).thenReturn(expiration);

            try (MockedStatic<BearerTokenResolver> resolver = mockStatic(BearerTokenResolver.class)) {
                resolver.when(() -> BearerTokenResolver.resolve(request)).thenReturn("jwt-token");

                authService.logout(request);
            }

            verify(tokenBlacklist, times(1)).add("jwt-token", expiration);
        }

        @Test
        @DisplayName("실패: Authorization 헤더에 토큰이 없으면 UNAUTHORIZED, 블랙리스트 등록도 하지 않는다")
        void noToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            try (MockedStatic<BearerTokenResolver> resolver = mockStatic(BearerTokenResolver.class)) {
                resolver.when(() -> BearerTokenResolver.resolve(request)).thenReturn(null);

                assertBusinessException(() -> authService.logout(request), ErrorCode.UNAUTHORIZED);
            }

            verifyNoInteractions(jwtTokenProvider, tokenBlacklist);
        }
    }

    @Nested
    @DisplayName("signUp")
    class SignUpTests {

        @Test
        @DisplayName("성공: 비밀번호를 인코딩해 Redis에 대기 상태로 저장하고, 저장된 것과 같은 인증코드로 메일을 보낸다")
        void success() {
            SignUp request = signUpRequest("홍길동", "user@test.com", "raw-password", "새닉네임",
                    "010-1234-5678", LocalDate.of(1995, 5, 5));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
            when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");

            authService.signUp(request);

            ArgumentCaptor<Object> savedCaptor = ArgumentCaptor.forClass(Object.class);
            verify(redisJsonStore, times(1)).save(anyString(), savedCaptor.capture(), eq(Duration.ofMinutes(10)));
            SignUp saved = (SignUp) savedCaptor.getValue();
            assertEquals("홍길동", saved.name());
            assertEquals("encoded-password", saved.password());
            assertEquals("새닉네임", saved.nickname());
            assertNotNull(saved.authCode());
            assertEquals(6, saved.authCode().length(), "인증코드는 6자리여야 한다");
            assertTrue(saved.authCode().matches("[A-Z0-9]{6}"), "인증코드는 대문자/숫자로만 구성되어야 한다");

            verify(mailService, times(1)).sendSignUpMessage(anyString(), eq(saved.authCode()));
        }

        @Test
        @DisplayName("성공: 닉네임 앞뒤 공백은 제거되어 저장된다")
        void trimsNickname() {
            SignUp request = signUpRequest("홍길동", "user@test.com", "raw-password", "  새닉네임  ",
                    "010-1234-5678", LocalDate.of(1995, 5, 5));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname("새닉네임")).thenReturn(false);

            authService.signUp(request);

            verify(userRepository).existsByNickname("새닉네임");
        }

        @Test
        @DisplayName("실패: 이미 가입된 이메일이면 ALREADY_EXISTS, 인증코드를 만들거나 메일을 보내지 않는다")
        void emailAlreadyExists() {
            SignUp request = signUpRequest("홍길동", "user@test.com", "raw-password", "새닉네임",
                    "010-1234-5678", LocalDate.of(1995, 5, 5));
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertBusinessException(() -> authService.signUp(request), ErrorCode.ALREADY_EXISTS);

            verifyNoInteractions(mailVerificationPolicy, redisJsonStore, mailService);
        }

        @Test
        @DisplayName("실패: 이미 쓰는 닉네임이면 ALREADY_EXISTS")
        void nicknameAlreadyExists() {
            SignUp request = signUpRequest("홍길동", "user@test.com", "raw-password", "중복닉네임",
                    "010-1234-5678", LocalDate.of(1995, 5, 5));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

            assertBusinessException(() -> authService.signUp(request), ErrorCode.ALREADY_EXISTS);

            verifyNoInteractions(mailVerificationPolicy, redisJsonStore, mailService);
        }

        @Test
        @DisplayName("실패: 인증메일 발송 제한(정책)에 걸리면 예외가 그대로 전파되고, 저장/발송은 하지 않는다")
        void sendingNotAllowed() {
            SignUp request = signUpRequest("홍길동", "user@test.com", "raw-password", "새닉네임",
                    "010-1234-5678", LocalDate.of(1995, 5, 5));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUEST, "인증번호는 60초 후에 다시 요청할 수 있습니다."))
                    .when(mailVerificationPolicy).validateSendingAllowed(anyString(), anyString());

            assertBusinessException(() -> authService.signUp(request), ErrorCode.TOO_MANY_REQUEST);

            verifyNoInteractions(redisJsonStore, mailService);
        }
    }

    @Nested
    @DisplayName("validateEmail")
    class ValidateEmailTests {

        private SignUp pending(String authCode) {
            return new SignUp("홍길동", "user@test.com", "encoded-password", "새닉네임",
                    "010-1234-5678", UserGender.values()[0], LocalDate.of(1995, 5, 5), authCode);
        }

        @Test
        @DisplayName("성공: 인증코드가 일치하면 대기 정보로 User를 생성/저장하고, Redis 키와 시도 횟수를 정리한다")
        void success() {
            SignUp pendingUser = pending("ABC123");
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(pendingUser);

            authService.validateEmail(emailValidationRequest("user@test.com", "ABC123"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).save(captor.capture());
            User saved = captor.getValue();
            assertEquals("홍길동", saved.getName());
            assertEquals("user@test.com", saved.getEmail());
            assertEquals("encoded-password", saved.getPassword());
            assertEquals("새닉네임", saved.getNickname());

            verify(redisJsonStore, times(1)).delete(anyString());
            verify(mailVerificationPolicy, times(1)).clearAttempts(anyString(), anyString());
        }

        @Test
        @DisplayName("실패: 인증 시도 횟수를 초과했으면 Redis 키를 지우고 AUTH_FAILED를 던진다. 유저는 생성하지 않는다")
        void tooManyAttempts() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(false);

            assertBusinessException(
                    () -> authService.validateEmail(emailValidationRequest("user@test.com", "ABC123")),
                    ErrorCode.AUTH_FAILED);

            verify(redisJsonStore, times(1)).delete(anyString());
            verify(mailVerificationPolicy, times(1)).clearAttempts(anyString(), anyString());
            verifyNoInteractions(userRepository);
            verify(redisJsonStore, never()).find(anyString(), any());
        }

        @Test
        @DisplayName("실패: 인증 정보가 만료/없으면 NOT_FOUND. 이 경우 Redis 정리는 하지 않는다 (현재 동작 문서화)")
        void pendingExpired() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(null);

            assertBusinessException(
                    () -> authService.validateEmail(emailValidationRequest("user@test.com", "ABC123")),
                    ErrorCode.NOT_FOUND);

            verify(redisJsonStore, never()).delete(anyString());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("실패: 인증코드가 다르면 AUTH_FAILED, 유저를 생성하지 않고 Redis 데이터도 남겨둔다")
        void codeMismatch() {
            SignUp pendingUser = pending("ABC123");
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(pendingUser);

            assertBusinessException(
                    () -> authService.validateEmail(emailValidationRequest("user@test.com", "WRONG!")),
                    ErrorCode.AUTH_FAILED);

            verifyNoInteractions(userRepository);
            verify(redisJsonStore, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("성공: 가입된 이메일이면 인증코드를 Redis에 저장하고 메일을 보낸다")
        void success() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            authService.changePassword(new PasswordChange("user@test.com", "UNUSED"));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(redisJsonStore, times(1)).save(anyString(), captor.capture(), eq(Duration.ofMinutes(10)));
            PasswordChange saved = (PasswordChange) captor.getValue();
            assertEquals(6, saved.authCode().length());

            verify(mailService, times(1)).sendPasswordMessage(anyString(), eq(saved.authCode()));
        }

        @Test
        @DisplayName("가입되지 않은 이메일: 예외 없이 조용히 끝난다 (이메일 존재 여부가 노출되지 않도록)")
        void unknownEmailIsSilent() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertDoesNotThrow(() -> authService.changePassword(new PasswordChange("nobody@test.com", "UNUSED")));

            verifyNoInteractions(redisJsonStore, mailService);
        }

        @Test
        @DisplayName("실패: 인증메일 발송 제한에 걸리면 예외가 그대로 전파되고, 가입 여부 조회조차 하지 않는다")
        void sendingNotAllowed() {
            doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUEST, "인증번호는 60초 후에 다시 요청할 수 있습니다."))
                    .when(mailVerificationPolicy).validateSendingAllowed(anyString(), anyString());

            assertBusinessException(
                    () -> authService.changePassword(new PasswordChange("user@test.com", "UNUSED")),
                    ErrorCode.TOO_MANY_REQUEST);

            verifyNoInteractions(userRepository, redisJsonStore, mailService);
        }
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePasswordTests {

        @Test
        @DisplayName("성공: 인증코드가 일치하면 비밀번호가 인코딩되어 저장되고 Redis/시도 횟수가 정리된다")
        void success() {
            User user = buildUser("user@test.com", "닉네임", "old-encoded");
            PasswordChange pending = new PasswordChange("user@test.com", "ABC123");
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(pending);
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("new-password!")).thenReturn("new-encoded");

            authService.validatePassword(passwordValidationRequest("user@test.com", "ABC123", "new-password!", "new-password!"));

            assertEquals("new-encoded", user.getPassword());
            verify(redisJsonStore, times(1)).delete(anyString());
            verify(mailVerificationPolicy, times(1)).clearAttempts(anyString(), anyString());
        }

        @Test
        @DisplayName("실패: 새 비밀번호와 확인 값이 다르면 INVALID_REQUEST, 다른 어떤 것도 조회/호출하지 않는다")
        void confirmMismatch() {
            assertBusinessException(
                    () -> authService.validatePassword(
                            passwordValidationRequest("user@test.com", "ABC123", "new-password!", "different!")),
                    ErrorCode.INVALID_REQUEST);

            verifyNoInteractions(mailVerificationPolicy, redisJsonStore, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증 시도 횟수를 초과했으면 Redis 키를 지우고 AUTH_FAILED를 던진다")
        void tooManyAttempts() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(false);

            assertBusinessException(
                    () -> authService.validatePassword(
                            passwordValidationRequest("user@test.com", "ABC123", "new-password!", "new-password!")),
                    ErrorCode.AUTH_FAILED);

            verify(redisJsonStore, times(1)).delete(anyString());
            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증 정보가 만료/없으면 NOT_FOUND")
        void pendingExpired() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(null);

            assertBusinessException(
                    () -> authService.validatePassword(
                            passwordValidationRequest("user@test.com", "ABC123", "new-password!", "new-password!")),
                    ErrorCode.NOT_FOUND);

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증코드가 다르면 AUTH_FAILED, 비밀번호를 바꾸지 않는다")
        void codeMismatch() {
            PasswordChange pending = new PasswordChange("user@test.com", "ABC123");
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(pending);

            assertBusinessException(
                    () -> authService.validatePassword(
                            passwordValidationRequest("user@test.com", "WRONG!", "new-password!", "new-password!")),
                    ErrorCode.AUTH_FAILED);

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증은 통과했지만 (요청 시점과 이메일이 달라져) 유저를 못 찾으면 AUTH_FAILED, 비밀번호를 바꾸지 않는다")
        void userNotFoundAtFinalStep() {
            PasswordChange pending = new PasswordChange("user@test.com", "ABC123");
            when(mailVerificationPolicy.isVerificationAttemptAllowed(anyString(), anyString())).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(pending);
            when(userRepository.findByEmailOrNickname("user@test.com")).thenReturn(Optional.empty());

            assertBusinessException(
                    () -> authService.validatePassword(
                            passwordValidationRequest("user@test.com", "ABC123", "new-password!", "new-password!")),
                    ErrorCode.AUTH_FAILED);

            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("addBlackList")
    class AddBlackListTests {

        @Test
        @DisplayName("전달받은 토큰/만료 시각 그대로 TokenBlacklist에 위임한다")
        void delegatesToTokenBlacklist() {
            Date expiration = new Date();

            authService.addBlackList("token-value", expiration);

            verify(tokenBlacklist, times(1)).add("token-value", expiration);
        }
    }

    @Nested
    @DisplayName("createCode")
    class CreateCodeTests {

        @Test
        @DisplayName("6자리, 대문자 A-Z와 숫자 0-9로만 구성된 코드를 만든다")
        void generatesSixCharAlphanumericCode() {
            for (int i = 0; i < 50; i++) {
                String code = authService.createCode();
                assertEquals(6, code.length());
                assertTrue(code.matches("[A-Z0-9]{6}"), "코드가 형식에 맞지 않음: " + code);
            }
        }
    }
}