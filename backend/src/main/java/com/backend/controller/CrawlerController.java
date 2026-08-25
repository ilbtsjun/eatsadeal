package com.backend.controller;

import com.backend.crawler.common.EventCreateDto;
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
    public List<EventCreateDto> testBhcCrawler() {
        List<EventCreateDto> list = bhcCrawler.crawl();
        for(EventCreateDto eventCreateDto : list){
            eventService.createEvent(eventCreateDto);
        }
        return list;
    }
}
