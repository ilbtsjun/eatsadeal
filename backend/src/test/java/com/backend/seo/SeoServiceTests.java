package com.backend.seo;

import com.backend.brand.entity.Brand;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import com.backend.seo.service.SeoService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceTests {

    @Mock
    private EventRepository eventRepository;

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
        Brand brand = mock(Brand.class);
        lenient().when(brand.getName()).thenReturn("BHC");
        Event event = Event.builder()
                .title(title).description(description)
                .url("https://example.com/events/1").img(img)
                .startDate(LocalDateTime.now()).brand(brand).isActive(true)
                .build();
        ReflectionTestUtils.setField(event, "id", 5L);
        return event;
    }

    @Test
    @DisplayName("미리보기: 이벤트 제목·설명·이미지를 og 태그에 담는다")
    void previewHasEventMeta() {
        Event event = event("치킨 1+1", "한 마리 사면 한 마리 더", "https://image.example.com/a.png");
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));

        SeoService.Preview preview = seoService.buildEventPreview(5L);

        assertTrue(preview.found());
        assertTrue(preview.html().contains("<meta property=\"og:title\" content=\"치킨 1+1 | 이츠어딜!\" />"));
        assertTrue(preview.html().contains("<meta property=\"og:description\" content=\"한 마리 사면 한 마리 더\" />"));
        assertTrue(preview.html().contains("<meta property=\"og:image\" content=\"https://image.example.com/a.png\" />"));
        assertTrue(preview.html().contains("<meta property=\"og:url\" content=\"https://www.eatsadeal.kr/events/5\" />"));
    }

    @Test
    @DisplayName("미리보기: 제목의 특수문자는 이스케이프해 태그가 깨지지 않는다")
    void previewEscapesHtml() {
        Event event = event("\"신메뉴\" <script>alert(1)</script> & 할인", "설명", "https://image.example.com/a.png");
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));

        String html = seoService.buildEventPreview(5L).html();

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&quot;신메뉴&quot; &lt;script&gt;alert(1)&lt;/script&gt; &amp; 할인"));
    }

    @Test
    @DisplayName("미리보기: 설명이 없으면 브랜드와 제목으로, https가 아닌 이미지는 대표 이미지로 대신한다")
    void previewFallbacks() {
        Event event = event("포장 할인", null, "http://insecure.example.com/a.png");
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));

        String html = seoService.buildEventPreview(5L).html();

        assertTrue(html.contains("<meta property=\"og:description\" content=\"BHC 이벤트: 포장 할인\" />"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://www.eatsadeal.kr/og-image.png\" />"));
    }

    @Test
    @DisplayName("미리보기: 긴 설명은 150자로 줄인다")
    void previewTruncatesLongDescription() {
        Event event = event("이벤트", "가".repeat(300), "https://image.example.com/a.png");
        when(eventRepository.findVisibleById(5L)).thenReturn(Optional.of(event));

        String html = seoService.buildEventPreview(5L).html();

        assertTrue(html.contains("content=\"" + "가".repeat(149) + "…\""));
    }

    @Test
    @DisplayName("미리보기: 없거나 숨긴 이벤트는 found=false와 사이트 기본 미리보기를 준다")
    void previewNotFound() {
        when(eventRepository.findVisibleById(99L)).thenReturn(Optional.empty());

        SeoService.Preview preview = seoService.buildEventPreview(99L);

        assertFalse(preview.found());
        assertTrue(preview.html().contains("<meta property=\"og:title\" content=\"이츠어딜! - 프랜차이즈 할인 이벤트 모음\" />"));
    }
}
