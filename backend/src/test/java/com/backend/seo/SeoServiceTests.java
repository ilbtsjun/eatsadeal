package com.backend.seo;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
