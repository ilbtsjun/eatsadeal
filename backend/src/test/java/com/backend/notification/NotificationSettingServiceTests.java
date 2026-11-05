package com.backend.notification;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.notification.dto.ConsentSource;
import com.backend.notification.dto.ConsentType;
import com.backend.notification.dto.GetConsentResponse;
import com.backend.notification.dto.UpdateConsentRequest;
import com.backend.notification.entity.ConsentHistory;
import com.backend.notification.entity.NotificationSetting;
import com.backend.notification.repository.ConsentHistoryRepository;
import com.backend.notification.repository.NotificationSettingRepository;
import com.backend.notification.service.NotificationSettingService;
import com.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTests {

    @Mock
    private ConsentHistoryRepository consentHistoryRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    private User mockUser() {
        return mock(User.class);
    }

    private User givenLoggedIn() {
        User user = mockUser();
        when(currentUserService.getRequiredUser()).thenReturn(user);
        return user;
    }

    private User givenLoggedInForUpdate(NotificationSetting setting) {
        User user = givenLoggedIn();
        when(user.getId()).thenReturn(1L);
        when(notificationSettingRepository.findByUserForUpdate(user)).thenReturn(Optional.ofNullable(setting));
        return user;
    }

    private NotificationSetting buildSetting(boolean marketingAgreed, boolean emailAgreed) {
        NotificationSetting setting = NotificationSetting.builder().user(mockUser()).build();
        if (marketingAgreed) {
            setting.agreeMarketing(true);
        }
        if (emailAgreed) {
            setting.agreeEmail(true);
        }
        return setting;
    }

    private List<ConsentHistory> captureHistories(int expectedCount) {
        ArgumentCaptor<ConsentHistory> captor = ArgumentCaptor.forClass(ConsentHistory.class);
        verify(consentHistoryRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private BusinessException givenNotLoggedIn() {
        BusinessException authException = new BusinessException(ErrorCode.UNAUTHORIZED);
        when(currentUserService.getRequiredUser()).thenThrow(authException);
        return authException;
    }

    @Nested
    @DisplayName("getMyConsents")
    class GetMyConsentsTests {

        @Test
        @DisplayName("성공: 설정이 없으면 저장하지 않고 모두 미동의(false, null)로 반환한다")
        void emptyWhenNoSetting() {
            User user = givenLoggedIn();
            when(notificationSettingRepository.findByUser(user)).thenReturn(Optional.empty());

            GetConsentResponse response = notificationSettingService.getMyConsents();

            assertFalse(response.marketingAgreed());
            assertNull(response.marketingAgreedAt());
            assertFalse(response.emailAgreed());
            assertNull(response.emailAgreedAt());
            verify(notificationSettingRepository, never()).save(any());
            verify(notificationSettingRepository, never()).insertIfAbsent(any());
        }

        @Test
        @DisplayName("성공: 설정이 있으면 저장된 동의 여부와 일시를 그대로 반환한다")
        void returnsSavedSetting() {
            User user = givenLoggedIn();
            NotificationSetting setting = buildSetting(true, false);
            when(notificationSettingRepository.findByUser(user)).thenReturn(Optional.of(setting));

            GetConsentResponse response = notificationSettingService.getMyConsents();

            assertTrue(response.marketingAgreed());
            assertEquals(setting.getMarketingAgreedAt(), response.marketingAgreedAt());
            assertFalse(response.emailAgreed());
            assertNull(response.emailAgreedAt());
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 설정을 조회하지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class, () -> notificationSettingService.getMyConsents());

            assertSame(authException, thrown);
            verifyNoInteractions(notificationSettingRepository, consentHistoryRepository);
        }
    }

    @Nested
    @DisplayName("updateConsents")
    class UpdateConsentsTests {

        @Test
        @DisplayName("성공: 처음 동의하면 설정 행을 보장하고 두 항목 모두 동의 처리, 이력 2건을 저장한다")
        void firstAgreeBoth() {
            NotificationSetting setting = buildSetting(false, false);
            User user = givenLoggedInForUpdate(setting);

            GetConsentResponse response = notificationSettingService.updateConsents(
                    new UpdateConsentRequest(true, true, ConsentSource.SUBSCRIBE_POPUP));

            verify(notificationSettingRepository).insertIfAbsent(1L);
            assertTrue(response.marketingAgreed());
            assertNotNull(response.marketingAgreedAt());
            assertTrue(response.emailAgreed());
            assertNotNull(response.emailAgreedAt());

            List<ConsentHistory> histories = captureHistories(2);
            assertEquals(ConsentType.MARKETING_USE, histories.get(0).getConsentType());
            assertEquals(ConsentType.AD_EMAIL, histories.get(1).getConsentType());
            histories.forEach(history -> {
                assertSame(user, history.getUser());
                assertTrue(history.getAgreed());
                assertEquals(ConsentSource.SUBSCRIBE_POPUP, history.getSource());
                assertNotNull(history.getCreatedAt());
            });
        }

        @Test
        @DisplayName("성공: 기존 값과 같으면 동의 일시를 바꾸지 않고 이력도 저장하지 않는다")
        void sameValuesKeepsHistory() {
            NotificationSetting setting = buildSetting(true, true);
            LocalDateTime marketingAgreedAt = setting.getMarketingAgreedAt();
            LocalDateTime emailAgreedAt = setting.getEmailAgreedAt();
            givenLoggedInForUpdate(setting);

            GetConsentResponse response = notificationSettingService.updateConsents(
                    new UpdateConsentRequest(true, true, ConsentSource.MYPAGE));

            assertEquals(marketingAgreedAt, response.marketingAgreedAt());
            assertEquals(emailAgreedAt, response.emailAgreedAt());
            verify(consentHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공: 한 항목만 바뀌면 그 항목의 이력 1건만 저장한다")
        void changeOnlyEmail() {
            NotificationSetting setting = buildSetting(true, false);
            LocalDateTime marketingAgreedAt = setting.getMarketingAgreedAt();
            givenLoggedInForUpdate(setting);

            GetConsentResponse response = notificationSettingService.updateConsents(
                    new UpdateConsentRequest(true, true, ConsentSource.MYPAGE));

            assertEquals(marketingAgreedAt, response.marketingAgreedAt());
            assertTrue(response.emailAgreed());
            assertNotNull(response.emailAgreedAt());

            List<ConsentHistory> histories = captureHistories(1);
            assertEquals(ConsentType.AD_EMAIL, histories.get(0).getConsentType());
            assertTrue(histories.get(0).getAgreed());
            assertEquals(ConsentSource.MYPAGE, histories.get(0).getSource());
        }

        @Test
        @DisplayName("성공: 철회하면 동의 일시가 null이 되고 철회 이력이 저장된다")
        void withdrawBoth() {
            NotificationSetting setting = buildSetting(true, true);
            givenLoggedInForUpdate(setting);

            GetConsentResponse response = notificationSettingService.updateConsents(
                    new UpdateConsentRequest(false, false, ConsentSource.MYPAGE));

            assertFalse(response.marketingAgreed());
            assertNull(response.marketingAgreedAt());
            assertFalse(response.emailAgreed());
            assertNull(response.emailAgreedAt());

            List<ConsentHistory> histories = captureHistories(2);
            histories.forEach(history -> assertFalse(history.getAgreed()));
        }

        @Test
        @DisplayName("실패: 설정 행을 보장한 뒤에도 조회되지 않으면 NOT_FOUND, 이력을 저장하지 않는다")
        void settingMissingAfterInsert() {
            givenLoggedInForUpdate(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> notificationSettingService.updateConsents(
                            new UpdateConsentRequest(true, true, ConsentSource.MYPAGE)));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(consentHistoryRepository);
        }

        @Test
        @DisplayName("실패: 로그인하지 않았으면 예외가 그대로 전파되고 설정/이력을 건드리지 않는다")
        void notLoggedIn() {
            BusinessException authException = givenNotLoggedIn();

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> notificationSettingService.updateConsents(
                            new UpdateConsentRequest(true, true, ConsentSource.MYPAGE)));

            assertSame(authException, thrown);
            verifyNoInteractions(notificationSettingRepository, consentHistoryRepository);
        }
    }

    @Nested
    @DisplayName("deleteConsents")
    class DeleteConsentsTests {

        @Test
        @DisplayName("성공: 해당 유저의 설정과 동의 이력을 모두 삭제한다")
        void deletesSettingAndHistory() {
            User user = mockUser();

            notificationSettingService.deleteConsents(user);

            verify(notificationSettingRepository).deleteByUser(user);
            verify(consentHistoryRepository).deleteByUser(user);
        }
    }
}
