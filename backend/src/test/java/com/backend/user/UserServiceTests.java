package com.backend.user;

import com.backend.auth.service.AuthService;
import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.user.dto.*;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import com.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    private static final String TOKEN = "access-token";

    private static final Date EXPIRATION = new Date(System.currentTimeMillis());

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    private User buildUser() {
        User user = User.builder()
                .name("기존이름")
                .email("user@test.com")
                .password("encoded-current")
                .nickname("기존닉네임")
                .phoneNumber("010-1111-2222")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private void givenLoggedIn(User user) {
        when(currentUserService.getRequiredUser()).thenReturn(user);
    }

    private void setStatus(User user, UserStatus status) {
        ReflectionTestUtils.setField(user, "userStatus", status);
    }

    private BusinessException assertBusinessException(Runnable runnable, ErrorCode expected) {
        BusinessException exception = assertThrows(BusinessException.class, runnable::run);
        assertEquals(expected, exception.getErrorCode());
        return exception;
    }

    private void assertBetween(LocalDateTime before, LocalDateTime actual, LocalDateTime after) {
        assertNotNull(actual);
        assertFalse(actual.isBefore(before), "기록 시각이 기대 구간보다 이르다: " + actual + " < " + before);
        assertFalse(actual.isAfter(after), "기록 시각이 기대 구간보다 늦다: " + actual + " > " + after);
    }

    private UpdateMyPage updateMyPageRequest(String nickname, String phoneNumber, String name, LocalDate birth) {
        return new UpdateMyPage(nickname, phoneNumber, name, birth);
    }

    private UpdatePassword updatePasswordRequest(String current, String update, String confirm) {
        return new UpdatePassword(current, update, confirm);
    }

    private SuspensionUser suspensionRequest(Long suspendTime, String reason, boolean status) {
        return new SuspensionUser(suspendTime, reason, status);
    }

    @Nested
    @DisplayName("isExistEmail / isExistNickname")
    class ExistTests {

        @Test
        @DisplayName("이메일 존재 여부를 repository 결과 그대로 반환한다")
        void isExistEmail() {
            when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);
            when(userRepository.existsByEmail("none@test.com")).thenReturn(false);

            assertTrue(userService.isExistEmail("exist@test.com"));
            assertFalse(userService.isExistEmail("none@test.com"));
        }

        @Test
        @DisplayName("닉네임 존재 여부를 repository 결과 그대로 반환한다")
        void isExistNickname() {
            when(userRepository.existsByNickname("사용중")).thenReturn(true);
            when(userRepository.existsByNickname("미사용")).thenReturn(false);

            assertTrue(userService.isExistNickname("사용중"));
            assertFalse(userService.isExistNickname("미사용"));
        }
    }

    @Nested
    @DisplayName("getMyPage / getUserInfo")
    class GetInfoTests {

        @Test
        @DisplayName("getMyPage 성공: 현재 로그인한 유저의 정보를 응답으로 만든다")
        void getMyPage() {
            givenLoggedIn(buildUser());

            GetMyPageResponse response = userService.getMyPage();

            assertNotNull(response);
            verify(currentUserService, times(1)).getRequiredUser();
        }

        @Test
        @DisplayName("getUserInfo 성공: ID로 조회한 유저의 정보를 응답으로 만든다 (현재 유저 조회는 하지 않음)")
        void getUserInfo() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser()));

            GetMyPageResponse response = userService.getUserInfo(1L);

            assertNotNull(response);
            verifyNoInteractions(currentUserService);
        }

        @Test
        @DisplayName("getUserInfo 실패: 존재하지 않는 유저면 NOT_FOUND")
        void getUserInfoNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(() -> userService.getUserInfo(999L), ErrorCode.NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("updateMyPage")
    class UpdateMyPageTests {

        @Test
        @DisplayName("성공: 이름/닉네임/전화번호/생일을 모두 수정하고 수정 시각이 기록된다")
        void updatesAllFields() {
            User user = buildUser();
            givenLoggedIn(user);
            when(userRepository.existsByNickname("새닉네임")).thenReturn(false);

            userService.updateMyPage(updateMyPageRequest("새닉네임", "010-9999-8888", "새이름", LocalDate.of(2000, 2, 2)));

            assertEquals("새이름", user.getName());
            assertEquals("새닉네임", user.getNickname());
            assertEquals("010-9999-8888", user.getPhoneNumber());
            assertEquals(LocalDate.of(2000, 2, 2), user.getBirth());
            assertNotNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("성공: 이름만 보내면 나머지(닉네임/전화번호/생일)는 기존 값이 유지되고 닉네임 중복 검사는 하지 않는다")
        void partialUpdate() {
            User user = buildUser();
            givenLoggedIn(user);

            userService.updateMyPage(updateMyPageRequest(null, null, "새이름", null));

            assertEquals("새이름", user.getName());
            assertEquals("기존닉네임", user.getNickname());
            assertEquals("010-1111-2222", user.getPhoneNumber());
            assertEquals(LocalDate.of(1990, 1, 1), user.getBirth());
            verify(userRepository, never()).existsByNickname(any());
        }

        @ParameterizedTest(name = "{index} - 값=[{0}]")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("성공: 문자열 필드가 빈 문자열/공백이면 기존 값을 유지한다")
        void blankStringsKeepOriginal(String blank) {
            User user = buildUser();
            givenLoggedIn(user);

            userService.updateMyPage(updateMyPageRequest(blank, blank, blank, null));

            assertEquals("기존이름", user.getName());
            assertEquals("기존닉네임", user.getNickname());
            assertEquals("010-1111-2222", user.getPhoneNumber());
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("성공: 현재와 같은 닉네임을 보내도 중복 검사 없이 수정된다 (자기 닉네임과 충돌하지 않음)")
        void sameNicknameSkipsDuplicateCheck() {
            User user = buildUser();
            givenLoggedIn(user);

            userService.updateMyPage(updateMyPageRequest("기존닉네임", null, "새이름", null));

            assertEquals("새이름", user.getName());
            verify(userRepository, never()).existsByNickname(any());
        }

        @ParameterizedTest(name = "{index} - 전화번호=[{0}]")
        @ValueSource(strings = {"010-1234-5678", "01012345678", "011-123-4567", "0161234567", "019-1234-5678"})
        @DisplayName("성공: 올바른 형식의 전화번호는 그대로 저장된다")
        void validPhoneNumbers(String phoneNumber) {
            User user = buildUser();
            givenLoggedIn(user);

            userService.updateMyPage(updateMyPageRequest(null, phoneNumber, null, null));

            assertEquals(phoneNumber, user.getPhoneNumber());
        }

        @ParameterizedTest(name = "{index} - 전화번호=[{0}]")
        @ValueSource(strings = {"02-1234-5678", "010-12-5678", "010-1234-567", "010-1234-56789",
                "012-1234-5678", "abc", "010 1234 5678", "+82-10-1234-5678"})
        @DisplayName("실패: 잘못된 형식의 전화번호면 INVALID_REQUEST, 어떤 필드도 바뀌지 않고 닉네임 검사도 하지 않는다")
        void invalidPhoneNumbers(String phoneNumber) {
            User user = buildUser();
            givenLoggedIn(user);

            assertBusinessException(
                    () -> userService.updateMyPage(updateMyPageRequest("새이름", "새닉네임", phoneNumber, null)),
                    ErrorCode.INVALID_REQUEST);

            assertEquals("기존이름", user.getName());
            assertEquals("기존닉네임", user.getNickname());
            assertEquals("010-1111-2222", user.getPhoneNumber());
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("실패: 전화번호 오류 메시지가 안내 문구와 같다")
        void invalidPhoneNumberMessage() {
            givenLoggedIn(buildUser());

            BusinessException exception = assertBusinessException(
                    () -> userService.updateMyPage(updateMyPageRequest(null, "abc", null, null)),
                    ErrorCode.INVALID_REQUEST);

            assertEquals("전화번호 형식이 올바르지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("실패: 다른 유저가 쓰는 닉네임이면 ALREADY_EXISTS, 어떤 필드도 바뀌지 않는다")
        void duplicateNickname() {
            User user = buildUser();
            givenLoggedIn(user);
            when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

            assertBusinessException(
                    () -> userService.updateMyPage(updateMyPageRequest("중복닉네임", null, "새이름", null)),
                    ErrorCode.ALREADY_EXISTS);

            assertEquals("기존이름", user.getName());
            assertEquals("기존닉네임", user.getNickname());
        }

        @Test
        @DisplayName("성공: 전화번호가 원래 없는 유저(null)도 이름만 수정할 수 있다 [현재 코드는 NPE로 실패]")
        void userWithoutPhoneNumberCanUpdateOtherFields() {
            User user = buildUser();
            ReflectionTestUtils.setField(user, "phoneNumber", null);
            givenLoggedIn(user);

            assertDoesNotThrow(() -> userService.updateMyPage(updateMyPageRequest(null, null, "새이름", null)));

            assertEquals("새이름", user.getName());
            assertNull(user.getPhoneNumber());
        }
    }

    // ==================================================================
    // updatePassword
    // ==================================================================
    @Nested
    @DisplayName("updatePassword")
    class UpdatePasswordTests {

        static Stream<Arguments> blankPasswordRequests() {
            return Stream.of(
                    Arguments.of("현재 비밀번호가 null", null, "new456!", "new456!"),
                    Arguments.of("현재 비밀번호가 빈 문자열", "", "new456!", "new456!"),
                    Arguments.of("현재 비밀번호가 공백", "   ", "new456!", "new456!"),
                    Arguments.of("변경할 비밀번호가 null", "current123!", null, "new456!"),
                    Arguments.of("변경할 비밀번호가 빈 문자열", "current123!", "", "new456!"),
                    Arguments.of("확인 비밀번호가 null", "current123!", "new456!", null),
                    Arguments.of("확인 비밀번호가 공백", "current123!", "new456!", " ")
            );
        }

        @Test
        @DisplayName("성공: 새 비밀번호를 인코딩해 저장하고 수정 시각이 기록된다")
        void success() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("current123!", "encoded-current")).thenReturn(true);
            when(passwordEncoder.matches("new456!", "encoded-current")).thenReturn(false);
            when(passwordEncoder.encode("new456!")).thenReturn("encoded-new");

            userService.updatePassword(updatePasswordRequest("current123!", "new456!", "new456!"));

            assertEquals("encoded-new", user.getPassword());
            assertNotNull(user.getUpdatedAt());
        }

        @ParameterizedTest(name = "{index} - {0}")
        @MethodSource("blankPasswordRequests")
        @DisplayName("실패: 세 값 중 하나라도 비어 있으면 INVALID_REQUEST, 인코더는 호출하지 않는다")
        void blankFields(String description, String current, String update, String confirm) {
            User user = buildUser();
            givenLoggedIn(user);

            assertBusinessException(
                    () -> userService.updatePassword(updatePasswordRequest(current, update, confirm)),
                    ErrorCode.INVALID_REQUEST);

            verifyNoInteractions(passwordEncoder);
            assertEquals("encoded-current", user.getPassword());
        }

        @Test
        @DisplayName("실패: 현재 비밀번호가 틀리면 PASSWORD_NOT_MATCHED, 새 비밀번호는 인코딩하지 않는다")
        void wrongCurrentPassword() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("wrong-password", "encoded-current")).thenReturn(false);

            assertBusinessException(
                    () -> userService.updatePassword(updatePasswordRequest("wrong-password", "new456!", "new456!")),
                    ErrorCode.PASSWORD_NOT_MATCHED);

            verify(passwordEncoder, never()).encode(any());
            assertEquals("encoded-current", user.getPassword());
        }

        @Test
        @DisplayName("실패: 새 비밀번호와 확인 값이 다르면 PASSWORD_NOT_MATCHED, 인코딩하지 않는다")
        void confirmMismatch() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("current123!", "encoded-current")).thenReturn(true);

            assertBusinessException(
                    () -> userService.updatePassword(updatePasswordRequest("current123!", "new456!", "different!")),
                    ErrorCode.PASSWORD_NOT_MATCHED);

            verify(passwordEncoder, never()).encode(any());
            assertEquals("encoded-current", user.getPassword());
        }

        @Test
        @DisplayName("실패: 새 비밀번호가 현재 비밀번호와 같으면 ALREADY_EXISTS, 인코딩하지 않는다")
        void sameAsCurrentPassword() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("current123!", "encoded-current")).thenReturn(true);

            assertBusinessException(
                    () -> userService.updatePassword(updatePasswordRequest("current123!", "current123!", "current123!")),
                    ErrorCode.ALREADY_EXISTS);

            verify(passwordEncoder, never()).encode(any());
            assertEquals("encoded-current", user.getPassword());
        }
    }

    // ==================================================================
    // quit
    // ==================================================================
    @Nested
    @DisplayName("quit")
    class QuitTests {

        @Test
        @DisplayName("성공: 토큰을 블랙리스트에 올리고 탈퇴 처리한다 (개인정보 마스킹, 비밀번호 삭제)")
        void success() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("raw-password", "encoded-current")).thenReturn(true);
            when(currentUserService.getTokenByUser()).thenReturn(TOKEN);
            when(currentUserService.getExpirationByUser()).thenReturn(EXPIRATION);

            userService.quit(new QuitUser("raw-password"));

            verify(authService, times(1)).addBlackList(TOKEN, EXPIRATION);
            assertEquals(UserStatus.WITHDRAWN, user.getUserStatus());
            assertEquals("withdrawn_1@deleted.local", user.getEmail());
            assertEquals("탈퇴한 사용자_1", user.getNickname());
            assertNull(user.getPhoneNumber());
            assertNull(user.getPassword());
        }

        @Test
        @DisplayName("실패: 비밀번호가 틀리면 PASSWORD_NOT_MATCHED, 블랙리스트 등록도 탈퇴 처리도 하지 않는다")
        void wrongPassword() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("wrong-password", "encoded-current")).thenReturn(false);

            assertBusinessException(() -> userService.quit(new QuitUser("wrong-password")), ErrorCode.PASSWORD_NOT_MATCHED);

            verifyNoInteractions(authService);
            verify(currentUserService, never()).getTokenByUser();
            assertEquals(UserStatus.ACTIVE, user.getUserStatus());
            assertEquals("user@test.com", user.getEmail());
            assertEquals("encoded-current", user.getPassword());
        }

        @Test
        @DisplayName("실패: 블랙리스트 등록에서 예외가 나면 그대로 전파되고 탈퇴 처리되지 않는다")
        void blacklistFails() {
            User user = buildUser();
            givenLoggedIn(user);
            when(passwordEncoder.matches("raw-password", "encoded-current")).thenReturn(true);
            when(currentUserService.getTokenByUser()).thenReturn(TOKEN);
            when(currentUserService.getExpirationByUser()).thenReturn(EXPIRATION);
            doThrow(new IllegalStateException("저장소 장애")).when(authService).addBlackList(TOKEN, EXPIRATION);

            assertThrows(IllegalStateException.class, () -> userService.quit(new QuitUser("raw-password")));

            assertEquals(UserStatus.ACTIVE, user.getUserStatus());
            assertEquals("encoded-current", user.getPassword());
        }
    }


    @Nested
    @DisplayName("suspensionUser")
    class SuspensionUserTests {

        @Test
        @DisplayName("정지 성공: ACTIVE 유저가 SUSPEND가 되고, 정지 기간(일)과 앞뒤 공백이 제거된 사유가 기록된다")
        void suspend() {
            User user = buildUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            LocalDateTime before = LocalDateTime.now();

            userService.suspensionUser(1L, suspensionRequest(30L, "  규정 위반  ", true));

            LocalDateTime after = LocalDateTime.now();
            assertEquals(UserStatus.SUSPEND, user.getUserStatus());
            assertBetween(before, user.getSuspendedAt(), after);
            assertBetween(before.plusDays(30), user.getSuspendedUntil(), after.plusDays(30));
            assertEquals("규정 위반", user.getSuspendingReason());
        }

        @ParameterizedTest(name = "{index} - 현재 상태 {0}")
        @EnumSource(value = UserStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
        @DisplayName("정지 실패: ACTIVE가 아닌 유저(이미 정지/탈퇴 등)는 INVALID_STATUS, 상태는 그대로")
        void suspendRequiresActive(UserStatus status) {
            User user = buildUser();
            setStatus(user, status);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertBusinessException(
                    () -> userService.suspensionUser(1L, suspensionRequest(30L, "규정 위반", true)),
                    ErrorCode.INVALID_STATUS);

            assertEquals(status, user.getUserStatus());
            assertNull(user.getSuspendedAt());
        }

        @Test
        @DisplayName("해제 성공: SUSPEND 유저가 ACTIVE가 되고, 만료 시각이 지워지며, 사유가 해제 사유로 바뀐다")
        void release() {
            User user = buildUser();
            user.suspend(7L, "이전 정지 사유");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.suspensionUser(1L, suspensionRequest(0L, "  오해 소명  ", false));

            assertEquals(UserStatus.ACTIVE, user.getUserStatus());
            assertNull(user.getSuspendedUntil());
            assertEquals("오해 소명", user.getSuspendingReason());
        }

        @ParameterizedTest(name = "{index} - 현재 상태 {0}")
        @EnumSource(value = UserStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUSPEND")
        @DisplayName("해제 실패: SUSPEND가 아닌 유저는 INVALID_STATUS, 상태는 그대로")
        void releaseRequiresSuspend(UserStatus status) {
            User user = buildUser();
            setStatus(user, status);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertBusinessException(
                    () -> userService.suspensionUser(1L, suspensionRequest(0L, "해제 사유", false)),
                    ErrorCode.INVALID_STATUS);

            assertEquals(status, user.getUserStatus());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저면 NOT_FOUND")
        void userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertBusinessException(
                    () -> userService.suspensionUser(999L, suspensionRequest(30L, "규정 위반", true)),
                    ErrorCode.NOT_FOUND);
        }
    }
}