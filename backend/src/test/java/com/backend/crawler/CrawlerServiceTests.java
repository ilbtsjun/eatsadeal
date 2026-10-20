package com.backend.crawler;

import com.backend.brand.entity.Brand;
import com.backend.brand.repository.BrandRepository;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.common.Crawler;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlTarget;
import com.backend.crawler.service.CrawlerService;
import com.backend.event.dto.CreateEvent;
import com.backend.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTests {

    private static final CreateEvent EVENT = new CreateEvent(
            "title", "desc", "https://example.com/1", "https://example.com/1.png",
            LocalDateTime.now(), null, 1L, true, null);

    @Mock private EventService eventService;
    @Mock private BrandRepository brandRepository;

    private final Map<CrawlTarget, Crawler> crawlers = new EnumMap<>(CrawlTarget.class);
    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        for (CrawlTarget target : CrawlTarget.values()) {
            Crawler crawler = mock(Crawler.class);
            lenient().when(crawler.getName()).thenReturn(target.getBrandName());
            lenient().when(crawler.crawl(any())).thenReturn(List.of(EVENT));
            crawlers.put(target, crawler);
        }
        crawlerService = new CrawlerService(eventService, brandRepository,
                new ArrayList<>(crawlers.values()));
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
    @DisplayName("기동: 크롤러가 하나라도 없으면 IllegalStateException")
    void constructor_missingCrawler_throws() {
        List<Crawler> withoutBhc = new ArrayList<>(crawlers.values());
        withoutBhc.remove(crawler(CrawlTarget.BHC));

        assertThrows(IllegalStateException.class,
                () -> new CrawlerService(eventService, brandRepository, withoutBhc));
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
}