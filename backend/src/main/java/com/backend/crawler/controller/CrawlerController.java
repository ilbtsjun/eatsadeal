package com.backend.crawler.controller;

import com.backend.crawler.target.chicken.BBQ;
import com.backend.crawler.target.chicken.KyoChonChicken;
import com.backend.event.dto.CreateEvent;
import com.backend.crawler.target.chicken.BHC;
import com.backend.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CrawlerController {
    private final BHC bhcCrawler;
    private final BBQ bbqCrawler;
    private final KyoChonChicken kyochonCrawler;
    private final EventService eventService;

    @GetMapping("/api/crawl/bhc")
    @Scheduled(cron = "0 0 0/6 * * *")
    public List<CreateEvent> testBhcCrawler() {
        List<CreateEvent> list = bhcCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @GetMapping("/api/crawl/bbq")
    @Scheduled(cron = "0 0 0/6 * * *")
    public List<CreateEvent> testBbqCrawler() {
        List<CreateEvent> list = bbqCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @GetMapping("/api/crawl/kyochon")
    @Scheduled(cron = "0 0 0/6 * * *")
    public List<CreateEvent> testkyochonCrawler() {
        List<CreateEvent> list = kyochonCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }
}
