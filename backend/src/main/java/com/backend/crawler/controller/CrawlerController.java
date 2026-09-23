package com.backend.crawler.controller;

import com.backend.crawler.target.chicken.*;
import com.backend.crawler.target.hamburger.*;
import com.backend.crawler.target.pizza.*;
import com.backend.event.dto.CreateEvent;
import com.backend.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CrawlerController {
    private final EventService eventService;

    private final BHC bhcCrawler;
    private final BBQ bbqCrawler;
    private final KyoChonChicken kyochonCrawler;
    private final Pelicana pelicanaCrawler;
    private final Goobne goobneCrawler;

    private final Dominos dominosCrawler;
    private final Papajohns papajohnsCrawler;
    private final Pizzamaru pizzamaruCrawler;
    private final Pizzaetang pizzaetangCrawler;
    private final Pizzaschool pizzaschoolCrawler;

    private final Burgerking burgerkingCrawler;
    private final Lotteria lotteriaCrawler;
    private final KFC kfcCrawler;
    private final Momstouch momstouchCrawler;
    private final Frankburger frankburgerCrawler;

    @PostMapping("/api/crawl")
    @Scheduled(cron = "0 0 0/6 * * *")
    public void testCrawler(){
        chicken();
        pizza();
        hamburger();
    }

    @PostMapping("/api/crawl/chicken")

    public void chicken(){
        testBbqCrawler();
        testBhcCrawler();
        testKyochonCrawler();
        testPelicanaCrawler();
        testGoobneCrawler();
    }

    @PostMapping("/api/crawl/pizza")
    public void pizza(){
        testDominosCrawler();
        testPapajohnsCrawler();
        testPizzamaruCrawler();
        testPizzaetangCrawler();
        testPizzaeschoolCrawler();
    }

    @PostMapping("/api/crawl/hamburger")
    public void hamburger(){
        testBurgerkingCrawler();
        testLotteriaCrawler();
        testKfcCrawler();
        testMomstouchCrawler();
        testFrankburgerCrawler();
    }

    @PostMapping("/api/crawl/bhc")
    public List<CreateEvent> testBhcCrawler() {
        List<CreateEvent> list = bhcCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/bbq")
    public List<CreateEvent> testBbqCrawler() {
        List<CreateEvent> list = bbqCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/kyochon")
    public List<CreateEvent> testKyochonCrawler() {
        List<CreateEvent> list = kyochonCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/pelicana")
    public List<CreateEvent> testPelicanaCrawler() {
        List<CreateEvent> list = pelicanaCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/goobne")
    public List<CreateEvent> testGoobneCrawler() {
        List<CreateEvent> list = goobneCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/dominos")
    public List<CreateEvent> testDominosCrawler() {
        List<CreateEvent> list = dominosCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/papajohns")
    public List<CreateEvent> testPapajohnsCrawler() {
        List<CreateEvent> list = papajohnsCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/pizzamaru")
    public List<CreateEvent> testPizzamaruCrawler() {
        List<CreateEvent> list = pizzamaruCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/pizzaetang")
    public List<CreateEvent> testPizzaetangCrawler() {
        List<CreateEvent> list = pizzaetangCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/pizzaschool")
    public List<CreateEvent> testPizzaeschoolCrawler() {
        List<CreateEvent> list = pizzaschoolCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/burgerking")
    public List<CreateEvent> testBurgerkingCrawler() {
        List<CreateEvent> list = burgerkingCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/lotteria")
    public List<CreateEvent> testLotteriaCrawler() {
        List<CreateEvent> list = lotteriaCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/kfc")
    public List<CreateEvent> testKfcCrawler() {
        List<CreateEvent> list = kfcCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/momstouch")
    public List<CreateEvent> testMomstouchCrawler() {
        List<CreateEvent> list = momstouchCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }

    @PostMapping("/api/crawl/frankburger")
    public List<CreateEvent> testFrankburgerCrawler() {
        List<CreateEvent> list = frankburgerCrawler.crawl();
        for(CreateEvent createEvent : list){
            eventService.upsertCrawledEvent(createEvent);
        }
        return list;
    }
}
