package com.backend.notification.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.common.log.CudLogging;
import com.backend.mail.service.MailService;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {
    private final ConsentHistoryRepository consentHistoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final CurrentUserService currentUserService;
    private final MailService mailService;

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

        Map<ConsentType, Boolean> changes = new LinkedHashMap<>();

        if (!setting.getMarketingAgreed().equals(request.marketingAgreed())) {
            setting.agreeMarketing(request.marketingAgreed());
            saveHistory(user, ConsentType.MARKETING_USE, request.marketingAgreed(), request.source());
            changes.put(ConsentType.MARKETING_USE, request.marketingAgreed());
        }

        if (!setting.getEmailAgreed().equals(request.emailAgreed())) {
            setting.agreeEmail(request.emailAgreed());
            saveHistory(user, ConsentType.AD_EMAIL, request.emailAgreed(), request.source());
            changes.put(ConsentType.AD_EMAIL, request.emailAgreed());
        }

        if (!changes.isEmpty()) {
            mailService.sendConsentResultMessage(user.getEmail(), changes, currentConsents(setting), LocalDateTime.now());
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
            mailService.sendConsentResultMessage(setting.getUser().getEmail(),
                    Map.of(ConsentType.AD_EMAIL, false), currentConsents(setting), LocalDateTime.now());
        }
    }

    @Transactional
    public int sendConsentReconfirmNotices(LocalDateTime runAt) {
        List<NotificationSetting> targets = notificationSettingRepository.findReconfirmTargets(runAt.minusYears(2));
        int sent = 0;
        for (NotificationSetting setting : targets) {
            User user = setting.getUser();
            boolean ok = mailService.sendConsentReconfirmMessage(user.getEmail(), user.getNickname(),
                    setting.getEmailAgreedAt(), setting.getUnsubscribeToken());
            if (ok) {
                setting.markConsentNotified(runAt);
                sent++;
            }
        }
        log.info("[CONSENT RECONFIRM] 대상 {}명, 성공 {}, 실패 {}", targets.size(), sent, targets.size() - sent);
        return sent;
    }

    private Map<ConsentType, Boolean> currentConsents(NotificationSetting setting) {
        return Map.of(ConsentType.MARKETING_USE, setting.getMarketingAgreed(),
                ConsentType.AD_EMAIL, setting.getEmailAgreed());
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
