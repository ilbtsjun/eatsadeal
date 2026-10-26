package com.backend.crawler.service;

import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.common.CrawlResult;
import com.backend.crawler.common.Crawler;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.crawler.entity.CrawlRun;
import com.backend.crawler.entity.CrawlStatus;
import com.backend.crawler.repository.CrawlRunRepository;
import com.backend.event.dto.CreateEvent;
import com.backend.event.dto.EventCodeBackFillResult;
import com.backend.event.dto.UpsertResult;
import com.backend.event.service.EventCodeBackfillService;
import com.backend.event.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CrawlerService {

    private enum BrandState { ACTIVE, INACTIVE, MISSING }
    private final CrawlRunRepository crawlRunRepository;

    private static final int CLASSIFY_PAGE_SIZE = 50;
    private static final int CLASSIFY_MAX_PAGES = 2;

    private final EventService eventService;
    private final EventCodeBackfillService eventCodeBackfillService;
    private final BrandRepository brandRepository;
    private final Map<CrawlTarget, Crawler> registry = new EnumMap<>(CrawlTarget.class);
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${app.crawler.parallelism:5}")
    private int parallelism = 5;

    @Value("${app.crawler.deactivate-missing:true}")
    private boolean deactivateMissing = true;

    @Value("${app.crawler.classify-after-crawl:true}")
    private boolean classifyAfterCrawl = true;

    public CrawlerService(EventService eventService,
                          EventCodeBackfillService eventCodeBackfillService,
                          BrandRepository brandRepository,
                          CrawlRunRepository crawlRunRepository,
                          List<Crawler> crawlers) {
        this.eventService = eventService;
        this.eventCodeBackfillService = eventCodeBackfillService;
        this.brandRepository = brandRepository;
        this.crawlRunRepository = crawlRunRepository;

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

    public List<CrawlRunResponse> getLatestRuns() {
        return crawlRunRepository.findTopByOrderByIdDesc()
                .map(last -> crawlRunRepository.findByRunIdOrderByIdAsc(last.getRunId()).stream()
                        .map(CrawlRunResponse::from)
                        .toList())
                .orElseGet(List::of);
    }

    public List<CrawlRunResponse> getRecentRuns(int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        return crawlRunRepository.findAllByOrderByIdDesc(PageRequest.of(0, size)).stream()
                .map(CrawlRunResponse::from)
                .toList();
    }

    private CrawlReport runExclusive(String label, List<CrawlTarget> targets) {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,
                    "이미 크롤링이 진행 중입니다. 완료 후 다시 시도해주세요.");
        }
        long start = System.currentTimeMillis();
        String runId = UUID.randomUUID().toString();
        int saved = 0;
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        List<String> empty = new ArrayList<>();
        try {
            Map<String, Brand> brands = loadBrands();

            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(parallelism, targets.size())));
            List<Future<TimedOutcome>> futures = new ArrayList<>();
            try {
                for (CrawlTarget target : targets) {
                    Brand brand = brands.get(target.getBrandName());
                    futures.add(pool.submit(() -> {
                        LocalDateTime startedAt = LocalDateTime.now();
                        CrawlOutcome outcome = crawlBrand(target, brand);
                        return new TimedOutcome(outcome, startedAt, LocalDateTime.now());
                    }));
                }

                for (int i = 0; i < targets.size(); i++) {
                    CrawlTarget target = targets.get(i);
                    Brand brand = brands.get(target.getBrandName());
                    TimedOutcome timed = await(futures.get(i), target);
                    CrawlOutcome outcome = timed.outcome();
                    saveHistory(runId, target, brand, timed);

                    saved += outcome.saved();
                    switch (outcome.status()) {
                        case SKIPPED_INACTIVE -> skipped.add(target.getKey());
                        case FAILED -> failed.add(target.getKey());
                        case PARTIAL -> partial.add(target.getKey());
                        case EMPTY -> empty.add(target.getKey());
                        case SUCCESS -> { }
                    }
                }
            } finally {
                pool.shutdownNow();
            }
            classifyPendingEvents();
            log.info("[CRAWL DONE] target={} runId={} saved={} skipped={} failed={} partial={} empty={} elapsed={}ms",
                    label, runId, saved, skipped, failed, partial, empty, System.currentTimeMillis() - start);
            return new CrawlReport(saved, skipped, failed, partial, empty);
        } finally {
            running.set(false);
        }
    }

    private void classifyPendingEvents() {
        if (!classifyAfterCrawl) {
            return;
        }
        try {
            Long afterId = null;
            int processed = 0;
            int classified = 0;
            for (int page = 0; page < CLASSIFY_MAX_PAGES; page++) {
                EventCodeBackFillResult result = eventCodeBackfillService.run(CLASSIFY_PAGE_SIZE, afterId);
                processed += result.processed();
                classified += result.classified();
                afterId = result.nextAfterId();
                if (afterId == null) {
                    break;
                }
            }
            if (processed > 0) {
                log.info("[CRAWL CLASSIFY] processed={} classified={}", processed, classified);
            }
        } catch (Exception e) {
            log.warn("[CRAWL CLASSIFY FAIL] reason={}", e.toString());
        }
    }

    private TimedOutcome await(Future<TimedOutcome> future, CrawlTarget target) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failedOutcome(target, e);
        } catch (ExecutionException e) {
            return failedOutcome(target, e.getCause() == null ? e : e.getCause());
        }
    }

    private TimedOutcome failedOutcome(CrawlTarget target, Throwable cause) {
        log.error("[CRAWL FAIL] brand={} reason={}", target.getKey(), cause.toString(), cause);
        LocalDateTime now = LocalDateTime.now();
        return new TimedOutcome(new CrawlOutcome(CrawlStatus.FAILED, 0, 0, 0,
                cause.getClass().getSimpleName() + ": " + cause.getMessage(), null, null, null), now, now);
    }

    private CrawlOutcome crawlBrand(CrawlTarget target, Brand brand) {
        BrandState state = stateOf(brand);
        if (state == BrandState.INACTIVE) {
            log.info("[CRAWL SKIP] brand={} reason=inactive", target.getKey());
            return new CrawlOutcome(CrawlStatus.SKIPPED_INACTIVE, 0, 0, 0, null, null, null, null);
        }
        if (state == BrandState.MISSING) {
            log.warn("[CRAWL FAIL] brand={} reason=brand not found", target.getKey());
            return new CrawlOutcome(CrawlStatus.FAILED, 0, 0, 0, "brand not found", null, null, null);
        }
        try {
            CrawlResult result = registry.get(target).crawl(brand);
            List<CreateEvent> list = result.events() == null ? List.of() : result.events();
            int collected = list.size();
            int crawlErrors = result.errors();
            int saveErrors = 0;
            int created = 0;
            int updated = 0;
            int unchanged = 0;
            String firstError = result.firstError();

            for (CreateEvent event : list) {
                try {
                    UpsertResult upsert = eventService.upsertCrawledEvent(event);
                    switch (upsert) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> unchanged++;
                    }
                } catch (Exception e) {
                    saveErrors++;
                    if (firstError == null) {
                        firstError = "저장 - " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    }
                    log.warn("[CRAWL SAVE FAIL] brand={} url={} reason={}",
                            target.getKey(), event.url(), e.getClass().getSimpleName());
                }
            }

            int totalErrors = crawlErrors + saveErrors;
            CrawlStatus status;
            if (collected == 0) {
                status = totalErrors > 0 ? CrawlStatus.FAILED : CrawlStatus.EMPTY;
            } else {
                status = totalErrors > 0 ? CrawlStatus.PARTIAL : CrawlStatus.SUCCESS;
            }
            if (deactivateMissing && status == CrawlStatus.SUCCESS) {
                deactivateMissingEvents(target, brand, list);
            }
            if (status == CrawlStatus.EMPTY) {
                log.warn("[CRAWL EMPTY] brand={} reason=no events collected", target.getKey());
            }
            log.info("[CRAWL OK] brand={} count={} status={} created={} updated={} unchanged={} crawlErrors={} saveErrors={}",
                    target.getKey(), collected, status, created, updated, unchanged, crawlErrors, saveErrors);
            return new CrawlOutcome(status, collected, saveErrors, crawlErrors, firstError,
                    created, updated, unchanged);
        } catch (Exception e) {
            log.error("[CRAWL FAIL] brand={} reason={}", target.getKey(), e.toString(), e);
            return new CrawlOutcome(CrawlStatus.FAILED, 0, 0, 0,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), null, null, null);
        }
    }

    private void deactivateMissingEvents(CrawlTarget target, Brand brand, List<CreateEvent> list) {
        try {
            int deactivated = eventService.deactivateMissing(brand.getId(),
                    list.stream().map(CreateEvent::url).toList());
            if (deactivated > 0) {
                log.info("[CRAWL DEACTIVATED] brand={} count={}", target.getKey(), deactivated);
            }
        } catch (Exception e) {
            log.warn("[CRAWL DEACTIVATE FAIL] brand={} reason={}", target.getKey(), e.toString());
        }
    }

    private void saveHistory(String runId, CrawlTarget target, Brand brand, TimedOutcome timed) {
        CrawlOutcome outcome = timed.outcome();
        try {
            CrawlRun run = CrawlRun.of(runId,
                    brand == null ? null : brand.getId(),
                    target.getKey(), outcome.status(),
                    outcome.collected(), outcome.errors(), outcome.message(),
                    timed.startedAt(), timed.finishedAt());
            run.recordCounts(outcome.created(), outcome.updated(), outcome.unchanged());
            crawlRunRepository.save(run);
        } catch (Exception e) {
            log.warn("[CRAWL RUN SAVE FAIL] brand={} reason={}", target.getKey(), e.getMessage());
        }
    }

    private record TimedOutcome(CrawlOutcome outcome, LocalDateTime startedAt, LocalDateTime finishedAt) {
    }

    private record CrawlOutcome(CrawlStatus status, int collected, int saveErrors, int crawlErrors, String message,
                                Integer created, Integer updated, Integer unchanged) {
        int saved() {
            return collected - saveErrors;
        }

        int errors() {
            return saveErrors + crawlErrors;
        }
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
