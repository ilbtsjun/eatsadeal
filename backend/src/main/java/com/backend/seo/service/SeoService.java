package com.backend.seo.service;

import com.backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeoService {
    private static final List<String> STATIC_PATHS = List.of("/", "/terms", "/privacy");

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
}
