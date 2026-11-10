package com.backend.seo;

import com.backend.brand.entity.Brand;
import com.backend.event.dto.EventCode;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.seo.service.SeoService;
import com.backend.seo.service.SpaIndexClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceTests {

    private static final String INDEX_HTML = """
            <!doctype html>
            <html lang="ko">
              <head>
                <title>이츠어딜! - 프랜차이즈 할인 이벤트 모음</title>
                <meta name="description" content="기본 설명" />
                <link rel="canonical" href="https://www.eatsadeal.kr/" />
                <meta property="og:url" content="https://www.eatsadeal.kr/" />
                <meta property="og:title" content="기본 제목" />
                <meta property="og:description" content="기본 설명" />
                <meta property="og:image" content="https://www.eatsadeal.kr/og-image.png" />
                <meta property="og:image:width" content="1200" />
                <meta property="og:image:height" content="630" />
                <meta name="twitter:title" content="기본 제목" />
                <meta name="twitter:description" content="기본 설명" />
                <meta name="twitter:image" content="https://www.eatsadeal.kr/og-image.png" />
                <script type="module" crossorigin src="/assets/index-abc.js"></script>
              </head>
              <body>
                <div id="root"></div>
              </body>
            </html>
            """;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SpaIndexClient spaIndexClient;

    @InjectMocks
    private SeoService seoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seoService, "siteUrl", "https://www.eatsadeal.kr");
    }

    @Test
    @DisplayName("사이트맵: 정적 페이지와 노출 중인 이벤트 상세 주소를 담는다")
    void sitemapContainsStaticPagesAndEvents() {
        when(eventRepository.findVisibleIds(any())).thenReturn(List.of(12L, 7L));

        String xml = seoService.buildSitemap();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
        assertTrue(xml.contains("<loc>https://www.eatsadeal.kr/</loc>"));
        assertTrue(xml.contains("<loc>https://www.eatsadeal.kr/terms</loc>"));
        assertTrue(xml.contains("<loc>https://www.eatsadeal.kr/privacy</loc>"));
        assertTrue(xml.contains("<loc>https://www.eatsadeal.kr/events/12</loc>"));
        assertTrue(xml.contains("<loc>https://www.eatsadeal.kr/events/7</loc>"));
        assertTrue(xml.trim().endsWith("</urlset>"));
    }

    @Test
    @DisplayName("사이트맵: 로그인이 필요한 페이지는 넣지 않는다")
    void sitemapExcludesPrivatePages() {
        when(eventRepository.findVisibleIds(any())).thenReturn(List.of());

        String xml = seoService.buildSitemap();

        assertFalse(xml.contains("/admin"));
        assertFalse(xml.contains("/mypage"));
        assertFalse(xml.contains("/favorites"));
        assertEquals(3, xml.split("<url>").length - 1);
    }

    private Event event(String title, String description, String img) {
        return event(title, description, img, null, Set.of());
    }

    private Event event(String title, String description, String img, LocalDateTime endDate, Set<EventCode> codes) {
        Brand brand = mock(Brand.class);
        lenient().when(brand.getName()).thenReturn("BHC");
        Event event = Event.builder()
                .title(title).description(description)
                .url("https://example.com/events/1").img(img)
                .startDate(LocalDateTime.of(2026, 8, 18, 4, 0)).endDate(endDate)
                .brand(brand).isActive(true).eventCodes(codes)
                .build();
        ReflectionTestUtils.setField(event, "id", 5L);
        return event;
    }

    private String pageHtml(Event event) {
        when(spaIndexClient.fetch()).thenReturn(Optional.of(INDEX_HTML));
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));
        return seoService.buildEventPage(5L).orElseThrow().html();
    }

    @Test
    @DisplayName("상세 페이지: index.html의 제목·설명·canonical·og 태그를 이벤트 값으로 바꾼다")
    void pageHasEventMeta() {
        Event event = event("치킨 1+1", "한 마리 사면 한 마리 더", "https://image.example.com/a.png");
        when(spaIndexClient.fetch()).thenReturn(Optional.of(INDEX_HTML));
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));

        SeoService.EventPage page = seoService.buildEventPage(5L).orElseThrow();
        String html = page.html();

        assertTrue(page.found());
        assertTrue(html.contains("<title>치킨 1+1 | 이츠어딜!</title>"));
        assertTrue(html.contains("<meta name=\"description\" content=\"한 마리 사면 한 마리 더\" />"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://www.eatsadeal.kr/events/5\" />"));
        assertTrue(html.contains("<meta property=\"og:url\" content=\"https://www.eatsadeal.kr/events/5\" />"));
        assertTrue(html.contains("<meta property=\"og:title\" content=\"치킨 1+1 | 이츠어딜!\" />"));
        assertTrue(html.contains("<meta property=\"og:description\" content=\"한 마리 사면 한 마리 더\" />"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://image.example.com/a.png\" />"));
        assertTrue(html.contains("<meta name=\"twitter:image\" content=\"https://image.example.com/a.png\" />"));
        assertFalse(html.contains("og:image:width"));
        assertFalse(html.contains("noindex"));
    }

    @Test
    @DisplayName("상세 페이지: 빌드 결과의 스크립트는 그대로 두고 root 안에 요약 본문을 넣는다")
    void pageKeepsScriptsAndAddsSummary() {
        Event event = event("치킨 1+1", "한 마리 사면 한 마리 더", "https://image.example.com/a.png",
                LocalDateTime.of(2026, 11, 30, 23, 59), Set.of(EventCode.TAKE_OUT, EventCode.BUY_ONE_GET_ONE));

        String html = pageHtml(event);

        assertTrue(html.contains("<script type=\"module\" crossorigin src=\"/assets/index-abc.js\"></script>"));
        assertTrue(html.contains("<div id=\"root\"><main><h1>치킨 1+1</h1>"));
        assertTrue(html.contains("<p>BHC · 1+1, 포장 할인</p>"));
        assertTrue(html.contains("<p>기간: 2026.08.18 ~ 2026.11.30</p>"));
        assertTrue(html.contains("href=\"https://example.com/events/1\" rel=\"nofollow noopener\""));
    }

    @Test
    @DisplayName("상세 페이지: 제목의 특수문자는 이스케이프해 태그가 깨지지 않는다")
    void pageEscapesHtml() {
        Event event = event("\"신메뉴\" <script>alert(1)</script> & 할인", "설명", "https://image.example.com/a.png");

        String html = pageHtml(event);

        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&quot;신메뉴&quot; &lt;script&gt;alert(1)&lt;/script&gt; &amp; 할인"));
    }

    @Test
    @DisplayName("상세 페이지: 설명이 없으면 브랜드·이벤트 종류·제목·기간으로, https가 아닌 이미지는 대표 이미지로 대신한다")
    void pageFallbacks() {
        Event event = event("포장 할인", null, "http://insecure.example.com/a.png",
                LocalDateTime.of(2099, 12, 31, 23, 59), Set.of(EventCode.TAKE_OUT));

        String html = pageHtml(event);

        assertTrue(html.contains("<meta property=\"og:description\" content=\"BHC 포장 할인 이벤트: 포장 할인 (2026.08.18 ~ 상시)\" />"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://www.eatsadeal.kr/og-image.png\" />"));
        assertTrue(html.contains("og:image:width"));
    }

    @Test
    @DisplayName("상세 페이지: 긴 설명은 150자로 줄인다")
    void pageTruncatesLongDescription() {
        Event event = event("이벤트", "가".repeat(300), "https://image.example.com/a.png");

        String html = pageHtml(event);

        assertTrue(html.contains("content=\"" + "가".repeat(149) + "…\""));
    }

    @Test
    @DisplayName("상세 페이지: 없거나 숨긴 이벤트는 found=false와 noindex를 주고 본문은 비워 둔다")
    void pageNotFound() {
        when(spaIndexClient.fetch()).thenReturn(Optional.of(INDEX_HTML));
        when(eventRepository.findVisibleById(99L)).thenReturn(Optional.empty());

        SeoService.EventPage page = seoService.buildEventPage(99L).orElseThrow();

        assertFalse(page.found());
        assertTrue(page.html().contains("<meta name=\"robots\" content=\"noindex\" />"));
        assertTrue(page.html().contains("<title>이벤트를 찾을 수 없음 | 이츠어딜!</title>"));
        assertTrue(page.html().contains("<div id=\"root\"></div>"));
    }

    @Test
    @DisplayName("상세 페이지: index.html을 가져오지 못하면 빈 값을 돌려준다 (nginx가 기본 index.html로 대신 응답)")
    void pageUnavailableWithoutIndexHtml() {
        when(spaIndexClient.fetch()).thenReturn(Optional.empty());

        assertTrue(seoService.buildEventPage(5L).isEmpty());
    }
}
