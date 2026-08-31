package com.backend.controller;

import com.backend.dto.CreateEvent;
import com.backend.crawler.target.BHC;
import com.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CrawlerController {
    private final BHC bhcCrawler;
    private final EventService eventService;

    @GetMapping("/api/crawl/bhc")
    public List<CreateEvent> testBhcCrawler() {
        List<CreateEvent> list = bhcCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.createEvent(createEvent);
        }
        return list;
    }
}
