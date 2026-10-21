package com.backend.crawler.scheduler;

import com.backend.common.error.BusinessException;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.crawler.entity.CrawlStatus;
import com.backend.crawler.service.CrawlerService;
import com.backend.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crawler.schedule-enabled", havingValue = "true", matchIfMissing = true)
public class CrawlerScheduler {

    private static final Set<CrawlStatus> ALERT_STATUSES =
            Set.of(CrawlStatus.FAILED, CrawlStatus.PARTIAL, CrawlStatus.EMPTY);

    private final CrawlerService crawlerService;
    private final MailService mailService;

    @Value("${app.crawler.alert-enabled:true}")
    private boolean alertEnabled;

    @Scheduled(cron = "${app.crawler.cron:0 0 0/6 * * *}", zone = "Asia/Seoul")
    public void crawlPeriodically() {
        log.info("[CRAWL SCHEDULE] 예약 크롤링 시작");
        try {
            CrawlReport report = crawlerService.crawlAll();
            alertIfNeeded(report);
        } catch (BusinessException e) {
            log.warn("[CRAWL SCHEDULE] 건너뜀: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[CRAWL SCHEDULE] 예약 크롤링 실패", e);
            if (alertEnabled) {
                mailService.sendCrawlErrorMessage(e);
            }
        }
    }

    private void alertIfNeeded(CrawlReport report) {
        if (!alertEnabled
                || (report.failed().isEmpty() && report.partial().isEmpty() && report.empty().isEmpty())) {
            return;
        }
        try {
            List<CrawlRunResponse> problems = crawlerService.getLatestRuns().stream()
                    .filter(run -> ALERT_STATUSES.contains(run.status()))
                    .toList();
            mailService.sendCrawlAlertMessage(report, problems);
        } catch (Exception e) {
            log.error("[CRAWL SCHEDULE] 알림 메일 준비 실패", e);
        }
    }
}
