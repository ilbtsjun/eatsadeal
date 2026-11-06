package com.backend.notification.scheduler;

import com.backend.mail.service.MailService;
import com.backend.notification.dto.EventDigest;
import com.backend.notification.service.BrandNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.schedule-enabled", havingValue = "true")
public class NotificationScheduler {
    private final BrandNotificationService brandNotificationService;
    private final MailService mailService;

    @Scheduled(cron = "${app.notification.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void sendDailyDigest() {
        log.info("[NOTIFICATION SCHEDULE] 구독 브랜드 알림 메일 발송 시작");
        try {
            LocalDateTime runAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            List<EventDigest> digests = brandNotificationService.buildDigests(runAt);
            int sent = 0;
            for (EventDigest digest : digests) {
                if (mailService.sendEventDigestMessage(digest)) {
                    sent++;
                }
            }
            log.info("[NOTIFICATION SCHEDULE] 완료: 대상 {}명, 성공 {}, 실패 {}", digests.size(), sent, digests.size() - sent);
        } catch (Exception e) {
            log.error("[NOTIFICATION SCHEDULE] 알림 메일 발송 실패", e);
        }
    }
}