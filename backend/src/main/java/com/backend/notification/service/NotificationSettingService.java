package com.backend.notification.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import com.backend.notification.dto.ConsentSource;
import com.backend.notification.dto.ConsentType;
import com.backend.notification.dto.GetConsentResponse;
import com.backend.notification.dto.UpdateConsentRequest;
import com.backend.notification.entity.ConsentHistory;
import com.backend.notification.entity.NotificationSetting;
import com.backend.notification.repository.ConsentHistoryRepository;
import com.backend.notification.repository.NotificationSettingRepository;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {
    private final ConsentHistoryRepository consentHistoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public GetConsentResponse getMyConsents() {
        User user = currentUserService.getRequiredUser();

        return notificationSettingRepository.findByUser(user)
                .map(GetConsentResponse::from)
                .orElseGet(GetConsentResponse::empty);
    }

    @Transactional
    @CudLogging("수신 동의 변경")
    public GetConsentResponse updateConsents(UpdateConsentRequest request) {
        User user = currentUserService.getRequiredUser();

        notificationSettingRepository.insertIfAbsent(user.getId(), NotificationSetting.newUnsubscribeToken());
        NotificationSetting setting = notificationSettingRepository.findByUserForUpdate(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!setting.getMarketingAgreed().equals(request.marketingAgreed())) {
            setting.agreeMarketing(request.marketingAgreed());
            saveHistory(user, ConsentType.MARKETING_USE, request.marketingAgreed(), request.source());
        }

        if (!setting.getEmailAgreed().equals(request.emailAgreed())) {
            setting.agreeEmail(request.emailAgreed());
            saveHistory(user, ConsentType.AD_EMAIL, request.emailAgreed(), request.source());
        }

        return GetConsentResponse.from(setting);
    }

    @Transactional
    public void deleteConsents(User user){
        notificationSettingRepository.deleteByUser(user);
        consentHistoryRepository.deleteByUser(user);
    }

    @Transactional
    public void unsubscribe(String token) {
        NotificationSetting setting = notificationSettingRepository.findByUnsubscribeToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 수신거부 링크입니다."));

        if (setting.getEmailAgreed()) {
            setting.agreeEmail(false);
            saveHistory(setting.getUser(), ConsentType.AD_EMAIL, false, ConsentSource.UNSUBSCRIBE_LINK);
        }
    }

    private void saveHistory(User user, ConsentType type, Boolean agreed, ConsentSource source) {
        consentHistoryRepository.save(ConsentHistory.builder()
                .user(user)
                .consentType(type)
                .agreed(agreed)
                .source(source)
                .build());
    }

}
