package com.backend.crawler.service;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.common.Crawler;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.crawler.target.chicken.*;
import com.backend.crawler.target.hamburger.*;
import com.backend.crawler.target.pizza.*;
import com.backend.event.dto.CreateEvent;
import com.backend.event.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class CrawlerService {

    private final EventService eventService;
    private final Map<CrawlTarget, Crawler> registry = new EnumMap<>(CrawlTarget.class);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CrawlerService(EventService eventService,
                          BHC bhc, BBQ bbq, KyoChonChicken kyochon, Pelicana pelicana, Goobne goobne,
                          Dominos dominos, Papajohns papajohns, Pizzamaru pizzamaru,
                          Pizzaetang pizzaetang, Pizzaschool pizzaschool,
                          Burgerking burgerking, Lotteria lotteria, KFC kfc,
                          Momstouch momstouch, Frankburger frankburger) {
        this.eventService = eventService;
        registry.put(CrawlTarget.BHC, bhc);
        registry.put(CrawlTarget.BBQ, bbq);
        registry.put(CrawlTarget.KYOCHON, kyochon);
        registry.put(CrawlTarget.PELICANA, pelicana);
        registry.put(CrawlTarget.GOOBNE, goobne);
        registry.put(CrawlTarget.DOMINOS, dominos);
        registry.put(CrawlTarget.PAPAJOHNS, papajohns);
        registry.put(CrawlTarget.PIZZAMARU, pizzamaru);
        registry.put(CrawlTarget.PIZZAETANG, pizzaetang);
        registry.put(CrawlTarget.PIZZASCHOOL, pizzaschool);
        registry.put(CrawlTarget.BURGERKING, burgerking);
        registry.put(CrawlTarget.LOTTERIA, lotteria);
        registry.put(CrawlTarget.KFC, kfc);
        registry.put(CrawlTarget.MOMSTOUCH, momstouch);
        registry.put(CrawlTarget.FRANKBURGER, frankburger);
    }

    public List<CreateEvent> crawlAll() {
        return runExclusive("ALL", List.of(CrawlTarget.values()));
    }

    public List<CreateEvent> crawlGroup(String group) {
        return runExclusive(group, CrawlTarget.byGroup(group));
    }

    public List<CreateEvent> crawlOne(CrawlTarget target) {
        return runExclusive(target.getKey(), List.of(target));
    }

    private List<CreateEvent> runExclusive(String label, List<CrawlTarget> targets) {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,
                    "이미 크롤링이 진행 중입니다. 완료 후 다시 시도해주세요.");
        }
        long start = System.currentTimeMillis();
        List<CreateEvent> collected = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        try {
            for (CrawlTarget target : targets) {
                try {
                    collected.addAll(crawlAndSave(target));
                } catch (Exception e) {
                    failed.add(target.getKey());
                    log.error("[CRAWL FAIL] brand={} reason={}", target.getKey(), e.getMessage(), e);
                }
            }
            log.info("[CRAWL DONE] target={} saved={} failed={} elapsed={}ms",
                    label, collected.size(), failed, System.currentTimeMillis() - start);
            return collected;
        } finally {
            running.set(false);
        }
    }

    private List<CreateEvent> crawlAndSave(CrawlTarget target) {
        List<CreateEvent> list = registry.get(target).crawl();
        if (list == null) {
            return List.of();
        }
        for (CreateEvent event : list) {
            eventService.upsertCrawledEvent(event);
        }
        log.info("[CRAWL OK] brand={} count={}", target.getKey(), list.size());
        return list;
    }
}
