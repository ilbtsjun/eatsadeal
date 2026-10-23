package com.backend.seo.service;

import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeoService {
    private static final List<String> STATIC_PATHS = List.of("/", "/terms", "/privacy");
    private static final String SITE_NAME = "이츠어딜!";
    private static final String DEFAULT_TITLE = SITE_NAME + " - 프랜차이즈 할인 이벤트 모음";
    private static final String DEFAULT_DESCRIPTION = "치킨, 피자, 햄버거 프랜차이즈의 할인·1+1·쿠폰 이벤트를 한곳에서 확인하세요.";
    private static final int MAX_DESCRIPTION_LENGTH = 150;

    public record Preview(boolean found, String html) {
    }

    private final EventRepository eventRepository;

    @Value("${app.site-url:https://www.eatsadeal.kr}")
    private String siteUrl;

    @Transactional(readOnly = true)
    public String buildSitemap() {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        STATIC_PATHS.forEach(path -> appendUrl(xml, path));
        eventRepository.findVisibleIds(LocalDateTime.now())
                .forEach(id -> appendUrl(xml, "/events/" + id));
        return xml.append("</urlset>\n").toString();
    }

    private void appendUrl(StringBuilder xml, String path) {
        xml.append("  <url><loc>").append(siteUrl).append(path).append("</loc></url>\n");
    }

    @Transactional(readOnly = true)
    public Preview buildEventPreview(Long eventId) {
        Optional<Event> found = eventRepository.findVisibleById(eventId);
        if (found.isEmpty()) {
            return new Preview(false, renderPreview(DEFAULT_TITLE, DEFAULT_DESCRIPTION, siteUrl + "/", defaultImage()));
        }
        Event event = found.get();
        String title = event.getTitle() + " | " + SITE_NAME;
        String description = StringUtils.hasText(event.getDescription())
                ? event.getDescription()
                : event.getBrand().getName() + " 이벤트: " + event.getTitle();
        String image = event.getImg() != null && event.getImg().startsWith("https://")
                ? event.getImg()
                : defaultImage();
        return new Preview(true, renderPreview(title, truncate(description), siteUrl + "/events/" + event.getId(), image));
    }

    private String renderPreview(String title, String description, String url, String image) {
        String t = escape(title);
        String d = escape(description);
        String u = escape(url);
        String i = escape(image);
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                <meta charset="UTF-8" />
                <title>%1$s</title>
                <meta name="description" content="%2$s" />
                <link rel="canonical" href="%3$s" />
                <meta property="og:type" content="website" />
                <meta property="og:site_name" content="%5$s" />
                <meta property="og:locale" content="ko_KR" />
                <meta property="og:url" content="%3$s" />
                <meta property="og:title" content="%1$s" />
                <meta property="og:description" content="%2$s" />
                <meta property="og:image" content="%4$s" />
                <meta name="twitter:card" content="summary_large_image" />
                <meta name="twitter:title" content="%1$s" />
                <meta name="twitter:description" content="%2$s" />
                <meta name="twitter:image" content="%4$s" />
                </head>
                <body>
                <h1>%1$s</h1>
                <p>%2$s</p>
                <a href="%3$s">이벤트 보러 가기</a>
                </body>
                </html>
                """.formatted(t, d, u, i, escape(SITE_NAME));
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private String defaultImage() {
        return siteUrl + "/og-image.png";
    }

    private static String truncate(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= MAX_DESCRIPTION_LENGTH ? oneLine : oneLine.substring(0, MAX_DESCRIPTION_LENGTH - 1) + "…";
    }
}
