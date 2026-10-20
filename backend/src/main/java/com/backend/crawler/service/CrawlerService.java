package com.backend.crawler.service;

import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.common.Crawler;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.event.dto.CreateEvent;
import com.backend.event.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class CrawlerService {

    private enum BrandState { ACTIVE, INACTIVE, MISSING }

    private final EventService eventService;
    private final BrandRepository brandRepository;
    private final Map<CrawlTarget, Crawler> registry = new EnumMap<>(CrawlTarget.class);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CrawlerService(EventService eventService,
                          BrandRepository brandRepository,
                          List<Crawler> crawlers) {
        this.eventService = eventService;
        this.brandRepository = brandRepository;

        Map<String, Crawler> byName = crawlers.stream()
                .collect(Collectors.toMap(Crawler::getName, Function.identity()));

        for (CrawlTarget target : CrawlTarget.values()) {
            Crawler crawler = byName.get(target.getBrandName());
            if (crawler == null) {
                throw new IllegalStateException("크롤러가 등록되지 않았습니다: " + target.getBrandName());
            }
            registry.put(target, crawler);
        }
    }

    public CrawlReport crawlAll() {
        return runExclusive("ALL", List.of(CrawlTarget.values()));
    }

    public CrawlReport crawlGroup(String group) {
        return runExclusive(group, CrawlTarget.byGroup(group));
    }

    public CrawlReport crawlOne(CrawlTarget target) {
        BrandState state = stateOf(loadBrands().get(target.getBrandName()));
        if (state == BrandState.MISSING) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "브랜드 정보를 찾을 수 없습니다: " + target.getKey());
        }
        if (state == BrandState.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "비활성화된 브랜드는 크롤링할 수 없습니다: " + target.getKey());
        }
        return runExclusive(target.getKey(), List.of(target));
    }

    private CrawlReport runExclusive(String label, List<CrawlTarget> targets) {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,
                    "이미 크롤링이 진행 중입니다. 완료 후 다시 시도해주세요.");
        }
        long start = System.currentTimeMillis();
        int saved = 0;
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        try {
            Map<String, Brand> brands = loadBrands();
            for (CrawlTarget target : targets) {
                Brand brand = brands.get(target.getBrandName());
                BrandState state = stateOf(brand);
                if (state == BrandState.INACTIVE) {
                    skipped.add(target.getKey());
                    log.info("[CRAWL SKIP] brand={} reason=inactive", target.getKey());
                    continue;
                }
                if (state == BrandState.MISSING) {
                    failed.add(target.getKey());
                    log.warn("[CRAWL FAIL] brand={} reason=brand not found", target.getKey());
                    continue;
                }
                try {
                    saved += crawlAndSave(target, brand);
                } catch (Exception e) {
                    failed.add(target.getKey());
                    log.error("[CRAWL FAIL] brand={} reason={}", target.getKey(), e.getMessage(), e);
                }
            }
            log.info("[CRAWL DONE] target={} saved={} skipped={} failed={} elapsed={}ms",
                    label, saved, skipped, failed, System.currentTimeMillis() - start);
            return new CrawlReport(saved, skipped, failed);
        } finally {
            running.set(false);
        }
    }

    private int crawlAndSave(CrawlTarget target, Brand brand) {
        List<CreateEvent> list = registry.get(target).crawl(brand);
        if (list == null) {
            return 0;
        }
        for (CreateEvent event : list) {
            eventService.upsertCrawledEvent(event);
        }
        log.info("[CRAWL OK] brand={} count={}", target.getKey(), list.size());
        return list.size();
    }

    private Map<String, Brand> loadBrands() {
        Map<String, Brand> map = new HashMap<>();
        for (Brand brand : brandRepository.findAll()) {
            map.put(brand.getName(), brand);
        }
        return map;
    }

    private BrandState stateOf(Brand brand) {
        if (brand == null) {
            return BrandState.MISSING;
        }
        return Boolean.TRUE.equals(brand.getIsActive()) ? BrandState.ACTIVE : BrandState.INACTIVE;
    }
}
