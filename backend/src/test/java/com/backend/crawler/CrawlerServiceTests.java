package com.backend.crawler;

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
import com.backend.crawler.scheduler.CrawlerScheduler;
import com.backend.crawler.service.CrawlerService;
import com.backend.event.dto.CreateEvent;
import com.backend.event.dto.EventCodeBackFillResult;
import com.backend.event.dto.UpsertResult;
import com.backend.event.service.EventCodeBackfillService;
import com.backend.event.service.EventService;
import com.backend.mail.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTests {

    private static final CreateEvent EVENT = new CreateEvent(
            "title", "desc", "https://example.com/1", "https://example.com/1.png",
            LocalDateTime.now(), null, 1L, true, null);

    @Mock private EventService eventService;
    @Mock private EventCodeBackfillService eventCodeBackfillService;
    @Mock private BrandRepository brandRepository;
    @Mock private CrawlRunRepository crawlRunRepository;
    @Mock private MailService mailService;

    private final Map<CrawlTarget, Crawler> crawlers = new EnumMap<>(CrawlTarget.class);
    private CrawlerService crawlerService;
    private CrawlerScheduler scheduler;

    @BeforeEach
    void setUp() {
        for (CrawlTarget target : CrawlTarget.values()) {
            Crawler crawler = mock(Crawler.class);
            lenient().when(crawler.getName()).thenReturn(target.getBrandName());
            lenient().when(crawler.crawl(any())).thenReturn(new CrawlResult(List.of(EVENT), 0, null));
            crawlers.put(target, crawler);
        }
        crawlerService = new CrawlerService(eventService, eventCodeBackfillService, brandRepository, crawlRunRepository,
                new ArrayList<>(crawlers.values()));
        lenient().when(eventService.upsertCrawledEvent(any(CreateEvent.class))).thenReturn(UpsertResult.CREATED);
        scheduler = new CrawlerScheduler(crawlerService, mailService);
        ReflectionTestUtils.setField(scheduler, "alertEnabled", true);
    }

    private Crawler crawler(CrawlTarget target) {
        return crawlers.get(target);
    }

    private void givenBrands(CrawlTarget... inactive) {
        List<CrawlTarget> inactiveList = List.of(inactive);
        List<Brand> brands = new ArrayList<>();
        for (CrawlTarget target : CrawlTarget.values()) {
            Brand brand = Brand.builder()
                    .name(target.getBrandName())
                    .url("https://" + target.getKey())
                    .img("img")
                    .build();
            if (inactiveList.contains(target)) {
                brand.deactive();
            }
            brands.add(brand);
        }
        when(brandRepository.findAll()).thenReturn(brands);
    }

    @Test
    @DisplayName("사라진 이벤트: 오류 없이 끝난 브랜드는 수집한 URL로 deactivateMissing을 호출한다")
    void crawlOne_success_deactivatesMissing() {
        givenBrands();

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventService).deactivateMissing(any(), eq(List.of(EVENT.url())));
    }

    @Test
    @DisplayName("사라진 이벤트: 수집 중 오류가 있으면(PARTIAL) 끄지 않는다")
    void crawlOne_partial_doesNotDeactivate() {
        givenBrands();
        when(crawler(CrawlTarget.BHC).crawl(any())).thenReturn(new CrawlResult(List.of(EVENT), 1, "boom"));

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventService, never()).deactivateMissing(any(), any());
    }

    @Test
    @DisplayName("사라진 이벤트: 아무것도 수집하지 못하면(EMPTY) 끄지 않는다")
    void crawlOne_empty_doesNotDeactivate() {
        givenBrands();
        when(crawler(CrawlTarget.BHC).crawl(any())).thenReturn(new CrawlResult(List.of(), 0, null));

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventService, never()).deactivateMissing(any(), any());
    }

    @Test
    @DisplayName("사라진 이벤트: 설정으로 끄면 호출하지 않는다")
    void crawlOne_switchOff_doesNotDeactivate() {
        givenBrands();
        ReflectionTestUtils.setField(crawlerService, "deactivateMissing", false);

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventService, never()).deactivateMissing(any(), any());
    }

    @Test
    @DisplayName("사라진 이벤트: 비활성화 중 예외가 나도 크롤링 결과는 SUCCESS로 남는다")
    void crawlOne_deactivateFails_crawlStillSucceeds() {
        givenBrands();
        when(eventService.deactivateMissing(any(), any())).thenThrow(new IllegalStateException("db"));

        CrawlReport report = crawlerService.crawlOne(CrawlTarget.BHC);

        assertEquals(1, report.saved());
        assertTrue(report.failed().isEmpty());
        assertTrue(report.partial().isEmpty());
    }

    @Test
    @DisplayName("크롤링 뒤: 코드가 없는 이벤트를 분류하고, 남은 이벤트가 있으면 다음 페이지도 처리한다")
    void crawlOne_classifiesPendingEvents_untilNoMore() {
        givenBrands();
        when(eventCodeBackfillService.run(50, null))
                .thenReturn(new EventCodeBackFillResult(50, 40, 77L, 10, 200));
        when(eventCodeBackfillService.run(50, 77L))
                .thenReturn(new EventCodeBackFillResult(3, 3, null, 13, 200));

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventCodeBackfillService).run(50, null);
        verify(eventCodeBackfillService).run(50, 77L);
        verifyNoMoreInteractions(eventCodeBackfillService);
    }

    @Test
    @DisplayName("크롤링 뒤: 한 번에 최대 2페이지까지만 분류한다")
    void crawlOne_classifiesPendingEvents_capsPages() {
        givenBrands();
        when(eventCodeBackfillService.run(anyInt(), any()))
                .thenReturn(new EventCodeBackFillResult(50, 0, 99L, 0, 200));

        crawlerService.crawlOne(CrawlTarget.BHC);

        verify(eventCodeBackfillService, times(2)).run(anyInt(), any());
    }

    @Test
    @DisplayName("크롤링 뒤: 설정으로 끄면 분류하지 않는다")
    void crawlOne_classifySwitchOff_doesNotClassify() {
        givenBrands();
        ReflectionTestUtils.setField(crawlerService, "classifyAfterCrawl", false);

        crawlerService.crawlOne(CrawlTarget.BHC);

        verifyNoInteractions(eventCodeBackfillService);
    }

    @Test
    @DisplayName("크롤링 뒤: 분류 중 예외가 나도 크롤링 결과는 그대로다")
    void crawlOne_classifyFails_crawlStillSucceeds() {
        givenBrands();
        when(eventCodeBackfillService.run(anyInt(), any())).thenThrow(new IllegalStateException("ai"));

        CrawlReport report = crawlerService.crawlOne(CrawlTarget.BHC);

        assertEquals(1, report.saved());
        assertTrue(report.failed().isEmpty());
    }

    @Test
    @DisplayName("기동: 크롤러가 하나라도 없으면 IllegalStateException")
    void constructor_missingCrawler_throws() {
        List<Crawler> withoutBhc = new ArrayList<>(crawlers.values());
        withoutBhc.remove(crawler(CrawlTarget.BHC));

        assertThrows(IllegalStateException.class,
                () -> new CrawlerService(eventService, eventCodeBackfillService, brandRepository, crawlRunRepository, withoutBhc));
    }

    @Test
    @DisplayName("전체 크롤링: 비활성 브랜드는 crawl(any())을 호출하지 않고 skipped에 담는다")
    void crawlAll_skipsInactiveBrand() {
        givenBrands(CrawlTarget.BHC);

        CrawlReport report = crawlerService.crawlAll();

        verify(crawler(CrawlTarget.BHC), never()).crawl(any());
        verify(crawler(CrawlTarget.BBQ)).crawl(any());
        assertEquals(14, report.saved());
        assertEquals(List.of("bhc"), report.skipped());
        assertTrue(report.failed().isEmpty());
        verify(eventService, times(14)).upsertCrawledEvent(any(CreateEvent.class));
    }

    @Test
    @DisplayName("카테고리 크롤링: 해당 카테고리의 비활성 브랜드만 건너뛴다")
    void crawlGroup_skipsInactiveBrandInGroup() {
        givenBrands(CrawlTarget.LOTTERIA);

        CrawlReport report = crawlerService.crawlGroup("hamburger");

        verify(crawler(CrawlTarget.LOTTERIA), never()).crawl(any());
        verify(crawler(CrawlTarget.BURGERKING)).crawl(any());
        verify(crawler(CrawlTarget.BHC), never()).crawl(any());
        assertEquals(4, report.saved());
        assertEquals(List.of("lotteria"), report.skipped());
    }

    @Test
    @DisplayName("단건 크롤링: 비활성 브랜드면 INVALID_STATUS 예외")
    void crawlOne_inactiveBrand_throws() {
        givenBrands(CrawlTarget.LOTTERIA);

        BusinessException e = assertThrows(BusinessException.class,
                () -> crawlerService.crawlOne(CrawlTarget.LOTTERIA));

        assertEquals(ErrorCode.INVALID_STATUS, e.getErrorCode());
        verify(crawler(CrawlTarget.LOTTERIA), never()).crawl(any());
    }

    @Test
    @DisplayName("단건 크롤링: DB에 브랜드가 없으면 NOT_FOUND 예외")
    void crawlOne_missingBrand_throws() {
        when(brandRepository.findAll()).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> crawlerService.crawlOne(CrawlTarget.BHC));

        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
        verify(crawler(CrawlTarget.BHC), never()).crawl(any());
    }

    @Test
    @DisplayName("단건 크롤링: 활성 브랜드는 정상 수집한다")
    void crawlOne_activeBrand_saves() {
        givenBrands();

        CrawlReport report = crawlerService.crawlOne(CrawlTarget.BHC);

        assertEquals(1, report.saved());
        assertTrue(report.skipped().isEmpty());
        assertTrue(report.failed().isEmpty());
        verify(eventService).upsertCrawledEvent(EVENT);
    }

    @Test
    @DisplayName("한 브랜드가 예외를 던져도 나머지는 계속 수집하고 failed에 담는다")
    void crawlAll_failureIsIsolated() {
        givenBrands();
        when(crawler(CrawlTarget.BHC).crawl(any())).thenThrow(new RuntimeException("boom"));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(14, report.saved());
        assertEquals(List.of("bhc"), report.failed());
        assertTrue(report.skipped().isEmpty());
    }

    @Test
    @DisplayName("크롤러에는 해당 브랜드의 Brand가 전달된다")
    void crawlOne_passesMatchingBrand() {
        givenBrands();

        crawlerService.crawlOne(CrawlTarget.BHC);

        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(crawler(CrawlTarget.BHC)).crawl(captor.capture());
        assertEquals("BHC", captor.getValue().getName());
    }

    @Test
    @DisplayName("전체 크롤링: 브랜드 조회는 실행당 1번만 한다")
    void crawlAll_loadsBrandsOnce() {
        givenBrands();

        crawlerService.crawlAll();

        verify(brandRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("수집 0건이면 EMPTY로 기록한다")
    void crawlAll_emptyResult_isReportedAsEmpty() {
        givenBrands();
        when(crawler(CrawlTarget.KFC).crawl(any())).thenReturn(new CrawlResult(List.of(), 0, null));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(List.of("kfc"), report.empty());
        assertEquals(14, report.saved());
    }

    @Test
    @DisplayName("저장 중 일부가 실패하면 PARTIAL이고 나머지는 계속 저장한다")
    void crawlAll_saveFailure_isPartial() {
        givenBrands();
        doThrow(new RuntimeException("boom")).when(eventService).upsertCrawledEvent(any(CreateEvent.class));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(15, report.partial().size());
        assertEquals(0, report.saved());
    }

    @Test
    @DisplayName("브랜드마다 실행 이력을 1건씩 저장한다 (건너뛴 브랜드 포함)")
    void crawlAll_savesOneHistoryPerTarget() {
        givenBrands(CrawlTarget.LOTTERIA);

        crawlerService.crawlAll();

        verify(crawlRunRepository, times(15)).save(any(CrawlRun.class));
    }

    @Test
    @DisplayName("이력 저장이 실패해도 크롤링 결과는 정상이다")
    void crawlAll_historyFailure_doesNotBreakCrawl() {
        givenBrands();
        when(crawlRunRepository.save(any(CrawlRun.class))).thenThrow(new RuntimeException("db down"));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(15, report.saved());
        assertTrue(report.failed().isEmpty());
    }

    @Test
    @DisplayName("수집은 됐지만 크롤링 중 오류가 있으면 PARTIAL")
    void crawlAll_crawlErrorsWithEvents_isPartial() {
        givenBrands();
        when(crawler(CrawlTarget.KFC).crawl(any()))
                .thenReturn(new CrawlResult(List.of(EVENT), 2, "항목 파싱"));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(List.of("kfc"), report.partial());
        assertEquals(15, report.saved());
    }

    @Test
    @DisplayName("수집 0건에 오류가 있으면 FAILED (EMPTY와 구분)")
    void crawlAll_crawlErrorsWithoutEvents_isFailed() {
        givenBrands();
        when(crawler(CrawlTarget.KFC).crawl(any()))
                .thenReturn(new CrawlResult(List.of(), 1, "목록 조회 status=503"));

        CrawlReport report = crawlerService.crawlAll();

        assertEquals(List.of("kfc"), report.failed());
        assertTrue(report.empty().isEmpty());
    }

    private CrawlRun history(String brandKey, CrawlStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return CrawlRun.of("run", 1L, brandKey, status, 0, 1, null, now, now);
    }

    @Test
    @DisplayName("스케줄러: 문제가 없으면 메일을 보내지 않는다")
    void scheduler_noProblem_sendsNothing() {
        givenBrands(CrawlTarget.LOTTERIA);

        scheduler.crawlPeriodically();

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("스케줄러: 일부 실패·0건이 있으면 문제 브랜드만 담아 메일을 보낸다")
    @SuppressWarnings("unchecked")
    void scheduler_problem_sendsMailWithProblemBrandsOnly() {
        givenBrands(CrawlTarget.LOTTERIA);
        when(crawler(CrawlTarget.KFC).crawl(any())).thenReturn(new CrawlResult(List.of(), 0, null));
        when(crawler(CrawlTarget.KYOCHON).crawl(any()))
                .thenReturn(new CrawlResult(List.of(EVENT), 2, "항목 파싱"));
        when(crawlRunRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(history("bhc", CrawlStatus.SUCCESS)));
        when(crawlRunRepository.findByRunIdOrderByIdAsc("run")).thenReturn(List.of(
                history("bhc", CrawlStatus.SUCCESS),
                history("kfc", CrawlStatus.EMPTY),
                history("kyochon", CrawlStatus.PARTIAL)));

        scheduler.crawlPeriodically();

        ArgumentCaptor<CrawlReport> report = ArgumentCaptor.forClass(CrawlReport.class);
        ArgumentCaptor<List<CrawlRunResponse>> problems = ArgumentCaptor.forClass(List.class);
        verify(mailService).sendCrawlAlertMessage(report.capture(), problems.capture());
        assertEquals(List.of("kyochon"), report.getValue().partial());
        assertEquals(List.of("kfc"), report.getValue().empty());
        assertEquals(List.of("kfc", "kyochon"),
                problems.getValue().stream().map(CrawlRunResponse::brandKey).toList());
    }

    @Test
    @DisplayName("스케줄러: 알림을 끄면 메일을 보내지 않는다")
    void scheduler_disabled_sendsNothing() {
        ReflectionTestUtils.setField(scheduler, "alertEnabled", false);
        givenBrands(CrawlTarget.LOTTERIA);
        when(crawler(CrawlTarget.KFC).crawl(any())).thenReturn(new CrawlResult(List.of(), 0, null));

        scheduler.crawlPeriodically();

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("스케줄러: 크롤링이 예외로 중단되면 실행 실패 메일을 보낸다")
    void scheduler_crawlException_sendsErrorMail() {
        when(brandRepository.findAll()).thenThrow(new RuntimeException("db down"));

        scheduler.crawlPeriodically();

        verify(mailService).sendCrawlErrorMessage(any(Exception.class));
        verify(mailService, never()).sendCrawlAlertMessage(any(), any());
    }

    @Test
    @DisplayName("스케줄러: 알림 준비 중 예외가 나도 실행 실패 메일로 오인하지 않는다")
    void scheduler_alertPreparationFailure_isSwallowed() {
        givenBrands(CrawlTarget.LOTTERIA);
        when(crawler(CrawlTarget.KFC).crawl(any())).thenReturn(new CrawlResult(List.of(), 0, null));
        when(crawlRunRepository.findTopByOrderByIdDesc()).thenThrow(new RuntimeException("db down"));

        scheduler.crawlPeriodically();

        verify(mailService, never()).sendCrawlErrorMessage(any());
        verify(mailService, never()).sendCrawlAlertMessage(any(), any());
    }

    @Test
    @DisplayName("이력에 신규·변경·동일 건수를 기록한다")
    void crawlOne_recordsUpsertCounts() {
        givenBrands();
        when(crawler(CrawlTarget.BHC).crawl(any())).thenReturn(
                new CrawlResult(List.of(EVENT, EVENT, EVENT), 0, null));
        when(eventService.upsertCrawledEvent(any(CreateEvent.class)))
                .thenReturn(UpsertResult.CREATED, UpsertResult.UPDATED, UpsertResult.UNCHANGED);

        crawlerService.crawlOne(CrawlTarget.BHC);

        ArgumentCaptor<CrawlRun> captor = ArgumentCaptor.forClass(CrawlRun.class);
        verify(crawlRunRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getCreatedCount());
        assertEquals(1, captor.getValue().getUpdatedCount());
        assertEquals(1, captor.getValue().getUnchangedCount());
    }

    @Test
    @DisplayName("건너뛴 브랜드의 이력에는 건수를 기록하지 않는다")
    void crawlAll_skippedBrand_hasNoUpsertCounts() {
        givenBrands(CrawlTarget.LOTTERIA);

        crawlerService.crawlAll();

        ArgumentCaptor<CrawlRun> captor = ArgumentCaptor.forClass(CrawlRun.class);
        verify(crawlRunRepository, times(15)).save(captor.capture());
        CrawlRun skipped = captor.getAllValues().stream()
                .filter(run -> run.getBrandKey().equals(CrawlTarget.LOTTERIA.getKey()))
                .findFirst().orElseThrow();
        assertNull(skipped.getCreatedCount());
    }
}
