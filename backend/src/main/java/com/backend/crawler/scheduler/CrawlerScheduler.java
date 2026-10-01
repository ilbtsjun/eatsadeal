package com.backend.crawler.scheduler;

import com.backend.common.error.BusinessException;
import com.backend.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crawler.schedule-enabled", havingValue = "true", matchIfMissing = true)
public class CrawlerScheduler {

    private final CrawlerService crawlerService;

    @Scheduled(cron = "${app.crawler.cron:0 0 0/6 * * *}", zone = "Asia/Seoul")
    public void crawlPeriodically() {
        log.info("[CRAWL SCHEDULE] 예약 크롤링 시작");
        try {
            crawlerService.crawlAll();
        } catch (BusinessException e) {
            log.warn("[CRAWL SCHEDULE] 건너뜀: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[CRAWL SCHEDULE] 예약 크롤링 실패", e);
        }
    }
}
