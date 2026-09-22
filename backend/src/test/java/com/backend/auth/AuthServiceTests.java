package com.backend.auth;

import com.backend.auth.dto.*;
import com.backend.auth.service.AuthService;
import com.backend.auth.service.MailVerificationPolicy;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.redis.RedisJsonStore;
import com.backend.mail.service.MailService;
import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    private static final String EMAIL = "user@test.com";
    private static final String TOKEN = "access-token";
    private static final Date EXPIRATION = new Date(System.currentTimeMillis() + 60_000);

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

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private User buildUser() {
        User user = User.builder().name("기존이름").email(EMAIL).password("encoded-password").nickname("기존닉네임").phoneNumber("010-1234-5678").birth(LocalDate.of(1990, 1, 1)).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private void setStatus(User user, UserStatus status) { ReflectionTestUtils.setField(user, "userStatus", status); }
    private BusinessException assertBusinessException(Runnable action, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(expected, exception.getErrorCode()); return exception;
    }
    private SignUp signUpRequest(String email, String nickname) {
        return new SignUp("홍길동"
                ,email
                ,"password123!"
                , nickname
                , "010-1234-5678"
                , null
                , LocalDate.of(2000, 1, 1), null);
    }
    private void stubVerificationAllowed() {
        doNothing().when(mailVerificationPolicy).validateSendingAllowed(anyString(), anyString());
    }

    @Nested
    @DisplayName("login")
    class LoginTests {
        @Test
        @DisplayName("성공: 이메일 또는 닉네임으로 조회하고 토큰을 발급한다")
        void success() {
            User user = buildUser();
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true);
            when(jwtTokenProvider.createToken(user)).thenReturn(TOKEN);
            LoginResponse response = authService.login(new LoginRequest(" " + EMAIL + " ", "password123!"));

            assertEquals("200", response.status());
            assertEquals(TOKEN, response.token());
            assertEquals("정상적으로 로그인 되었습니다.", response.msg());

            verify(userRepository).findByEmailOrNickname(EMAIL);
            verify(jwtTokenProvider).createToken(user);
        }

        @Test @DisplayName("실패: 존재하지 않는 아이디면 LOGIN_FAILED")
        void userNotFound() {
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.empty());
            assertBusinessException(() -> authService.login(new LoginRequest(EMAIL, "password123!")),
                    ErrorCode.LOGIN_FAILED);
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        @DisplayName("실패: 비밀번호가 틀리면 LOGIN_FAILED")
        void wrongPassword() {
            User user = buildUser();
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

            assertBusinessException(() -> authService.login(new LoginRequest(EMAIL, "wrong")), ErrorCode.LOGIN_FAILED);

            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("실패: 정지 유저는 403과 정지 정보를 반환하고 토큰을 발급하지 않는다")
        void suspended() {
            User user = buildUser();
            user.suspend(30L, "규정 위반");
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123!", user.getPassword())).thenReturn(true);
            LoginResponse response = authService.login(new LoginRequest(EMAIL, "password123!"));

            assertEquals("403", response.status()); assertNull(response.token()); assertTrue(response.msg().contains("규정 위반")); verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("실패: 탈퇴 유저는 403과 탈퇴 메시지를 반환한다")
        void withdrawn() {
            User user = buildUser();
            setStatus(user, UserStatus.WITHDRAWN);
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123!", user.getPassword())).thenReturn(true);
            LoginResponse response = authService.login(new LoginRequest(EMAIL, "password123!"));

            assertEquals("403", response.status());
            assertEquals("이미 탈퇴한 사용자입니다.", response.msg());
            assertNull(response.token());

            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {
        @Test
        @DisplayName("성공: Bearer 토큰을 블랙리스트에 등록한다")
        void success() {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
            when(jwtTokenProvider.getExpiration(TOKEN)).thenReturn(EXPIRATION);

            authService.logout(request);

            verify(tokenBlacklist).add(TOKEN, EXPIRATION);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Basic token", "Bearer ", "token"})
        @DisplayName("실패: 유효하지 않은 인증 헤더면 UNAUTHORIZED")
        void invalidHeader(String header) {
            when(request.getHeader("Authorization")).thenReturn(header);

            assertBusinessException(() -> authService.logout(request), ErrorCode.UNAUTHORIZED);

            verifyNoInteractions(jwtTokenProvider, tokenBlacklist);
        }
    }

    @Nested @DisplayName("signUp")
    class SignUpTests {
        @Test @DisplayName("성공: 중복 검사 후 비밀번호를 암호화하고 임시 회원 정보를 저장한 뒤 메일을 발송한다")
        void success() {
            SignUp request = signUpRequest(" User@Test.com ", " 새닉네임 ");
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
            when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");
            stubVerificationAllowed();

            authService.signUp(request);
            ArgumentCaptor<SignUp> captor = ArgumentCaptor.forClass(SignUp.class);

            verify(redisJsonStore).save(anyString(), captor.capture(), eq(Duration.ofMinutes(10)));
            verify(mailService).sendSignUpMessage(eq(EMAIL),
                    argThat(code -> code != null && Pattern.matches("[A-Z0-9]{6}", code)));

            SignUp pending = captor.getValue();

            assertEquals(EMAIL, pending.email());
            assertEquals("새닉네임", pending.nickname());
            assertEquals("encoded-password", pending.password());
            assertNotNull(pending.authCode());
        }

        @Test
        @DisplayName("실패: 이메일 또는 닉네임이 중복이면 ALREADY_EXISTS")
        void duplicate() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertBusinessException(() -> authService.signUp(signUpRequest(EMAIL, "닉네임")),
                    ErrorCode.ALREADY_EXISTS);

            verifyNoInteractions(mailVerificationPolicy, passwordEncoder, redisJsonStore, mailService);
        }
    }

    @Nested @DisplayName("validateEmail")
    class ValidateEmailTests {
        private final String KEY = "pending:user:";
        private SignUp pending(String code) {
            return new SignUp(" 홍길동 ",
                    EMAIL,
                    "encoded-password",
                    " 닉네임 ",
                    "010-1234-5678",
                    null,
                    LocalDate.of(2000, 1, 1),
                    code);
        }

        @Test @DisplayName("성공: 인증번호가 일치하면 유저를 저장하고 임시 정보를 삭제한다")
        void success() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("SIGNUP", EMAIL)).thenReturn(true); when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(pending("ABC123"));
            authService.validateEmail(new EmailValification(EMAIL, "ABC123"));
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            verify(userRepository).save(captor.capture());
            verify(redisJsonStore).delete(anyString());
            verify(mailVerificationPolicy).clearAttempts("SIGNUP", EMAIL);

            assertEquals("홍길동", captor.getValue().getName());
            assertEquals(EMAIL, captor.getValue().getEmail());
            assertEquals("닉네임", captor.getValue().getNickname());
        }

        @Test
        @DisplayName("실패: 인증 정보가 없으면 NOT_FOUND")
        void pendingNotFound() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("SIGNUP", EMAIL)).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(null);

            BusinessException exception = assertBusinessException(
                    () -> authService.validateEmail(new EmailValification(EMAIL, "ABC123")),
                    ErrorCode.NOT_FOUND);

            assertEquals("인증 시간이 만료되었거나 요청 정보가 존재하지 않습니다.", exception.getMessage());

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("실패: 인증번호가 다르면 AUTH_FAILED")
        void wrongCode() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("SIGNUP", EMAIL)).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(SignUp.class))).thenReturn(pending("ABC123"));

            assertBusinessException(() -> authService.validateEmail(new EmailValification(EMAIL, "WRONG1")),
                    ErrorCode.AUTH_FAILED);

            verifyNoInteractions(userRepository);
            verify(redisJsonStore, never()).delete(anyString());
        }

        @Test
        @DisplayName("실패: 인증 시도 횟수를 초과하면 임시 정보를 삭제하고 AUTH_FAILED")
        void attemptsExceeded() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("SIGNUP", EMAIL)).thenReturn(false);

            BusinessException exception = assertBusinessException(
                    () -> authService.validateEmail(new EmailValification(EMAIL, "ABC123")),
                    ErrorCode.AUTH_FAILED);

            assertEquals("인증번호 입력 횟수를 초과했습니다. 다시 회원가입을 진행해주세요.", exception.getMessage());

            verify(redisJsonStore).delete(anyString());
            verify(mailVerificationPolicy).clearAttempts("SIGNUP", EMAIL);
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {
        @Test
        @DisplayName("성공: 가입된 이메일이면 인증번호를 저장하고 메일을 발송한다")
        void success() {
            stubVerificationAllowed(); when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            authService.changePassword(new PasswordChange(EMAIL, null));

            verify(redisJsonStore).save(anyString(),
                    argThat(value -> value instanceof PasswordChange),
                    eq(Duration.ofMinutes(10)));
            verify(mailService).sendPasswordMessage(eq(EMAIL),
                    argThat(code -> code != null && Pattern.matches("[A-Z0-9]{6}", code)));
        }

        @Test
        @DisplayName("성공: 가입되지 않은 이메일이면 메일을 발송하지 않는다")
        void unknownEmail() {
            stubVerificationAllowed();

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

            authService.changePassword(new PasswordChange(EMAIL, null));

            verifyNoInteractions(redisJsonStore, mailService);
        }

        @Test
        @DisplayName("실패: 발송 정책 위반이면 이후 로직을 실행하지 않는다")
        void sendingNotAllowed() {
            doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUEST))
                    .when(mailVerificationPolicy)
                    .validateSendingAllowed(anyString(), anyString());

            assertBusinessException(() -> authService.changePassword(new PasswordChange(EMAIL, null)),
                    ErrorCode.TOO_MANY_REQUEST);

            verifyNoInteractions(userRepository, redisJsonStore, mailService);
        }
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePasswordTests {
        private PasswordValification request(String code, String password, String confirm) {
            return new PasswordValification(EMAIL, code, password, confirm);
        }
        private PasswordChange pending(String code) {
            return new PasswordChange(EMAIL, code);
        }

        @Test
        @DisplayName("성공: 인증번호가 일치하면 새 비밀번호를 저장하고 임시 정보를 삭제한다")
        void success() {
            User user = buildUser();
            when(mailVerificationPolicy.isVerificationAttemptAllowed("PASSWORD_RESET", EMAIL)).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(pending("ABC123"));
            when(userRepository.findByEmailOrNickname(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

            authService.validatePassword(request("ABC123", "new-password", "new-password"));

            assertEquals("encoded-new", user.getPassword());

            verify(redisJsonStore).delete(anyString());
            verify(mailVerificationPolicy).clearAttempts("PASSWORD_RESET", EMAIL);
        }

        @Test
        @DisplayName("실패: 비밀번호와 확인 값이 다르면 INVALID_REQUEST")
        void confirmMismatch() {
            assertBusinessException(() -> authService.validatePassword(
                    request("ABC123", "new-password", "different")), ErrorCode.INVALID_REQUEST);

            verifyNoInteractions(mailVerificationPolicy, redisJsonStore, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증 정보가 없으면 NOT_FOUND")
        void pendingNotFound() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("PASSWORD_RESET", EMAIL)).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(null);

            assertBusinessException(() -> authService.validatePassword(
                    request("ABC123", "new-password", "new-password")),
                    ErrorCode.NOT_FOUND);

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증번호가 다르면 AUTH_FAILED")
        void wrongCode() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("PASSWORD_RESET", EMAIL)).thenReturn(true);
            when(redisJsonStore.find(anyString(), eq(PasswordChange.class))).thenReturn(pending("ABC123"));

            assertBusinessException(() -> authService.validatePassword(
                    request("WRONG1", "new-password", "new-password")), ErrorCode.AUTH_FAILED);

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("실패: 인증 시도 횟수를 초과하면 AUTH_FAILED")
        void attemptsExceeded() {
            when(mailVerificationPolicy.isVerificationAttemptAllowed("PASSWORD_RESET", EMAIL)).thenReturn(false);

            assertBusinessException(() -> authService.validatePassword(
                    request("ABC123", "new-password", "new-password"))
                    , ErrorCode.AUTH_FAILED);

            verify(redisJsonStore).delete(anyString());
            verify(mailVerificationPolicy).clearAttempts("PASSWORD_RESET", EMAIL);
            verifyNoInteractions(userRepository, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("addBlackList / createCode")
    class EtcTests {
        @Test
        @DisplayName("addBlackList는 TokenBlacklist에 위임한다")
        void addBlackList() {
            authService.addBlackList(TOKEN, EXPIRATION);

            verify(tokenBlacklist).add(TOKEN, EXPIRATION);
        }

        @Test
        @DisplayName("createCode는 대문자와 숫자 6자리 문자열을 반환한다")
        void createCode() {
            String code = authService.createCode();

            assertNotNull(code);
            assertTrue(Pattern.matches("[A-Z0-9]{6}", code));
        }
    }
}
