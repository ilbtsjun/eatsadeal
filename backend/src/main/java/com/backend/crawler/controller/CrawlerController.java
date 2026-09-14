package com.backend.crawler.controller;

import com.backend.crawler.target.chicken.BBQ;
import com.backend.crawler.target.chicken.KyoChonChicken;
import com.backend.crawler.target.chicken.Pelicana;
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
    private final Pelicana pelicanaCrawler;
    private final EventService eventService;

    @GetMapping("/api/crawl")
    @Scheduled(cron = "0 0 0/6 * * *")
    public void testCrawler(){
        testBbqCrawler();
        testBhcCrawler();
        testKyochonCrawler();
        testPelicanaCrawler();
    }

    @GetMapping("/api/crawl/bhc")
    public List<CreateEvent> testBhcCrawler() {
        List<CreateEvent> list = bhcCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @GetMapping("/api/crawl/bbq")
    public List<CreateEvent> testBbqCrawler() {
        List<CreateEvent> list = bbqCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @GetMapping("/api/crawl/kyochon")
    public List<CreateEvent> testKyochonCrawler() {
        List<CreateEvent> list = kyochonCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @GetMapping("/api/crawl/pelicana")
    public List<CreateEvent> testPelicanaCrawler() {
        List<CreateEvent> list = pelicanaCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }
}
