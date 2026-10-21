package com.backend.crawler.common;

import com.backend.event.dto.CreateEvent;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CrawlerUtil {

    public static final LocalDate OPEN_ENDED_DATE = LocalDate.of(2099, 12, 31);

    private CrawlerUtil() {
    }

    public static String cleanText(String html) {
        if (html == null) {
            return "";
        }
        String noTags = html.replaceAll("<[^>]*>", " ");
        return HtmlUtils.htmlUnescape(noTags).replaceAll("\\s+", " ").trim();
    }

    public static String absoluteUrl(String baseUrl, String ref) {
        if (ref == null || ref.isBlank()) {
            return "";
        }
        if (ref.startsWith("//")) {
            return "https:" + ref;
        }
        if (ref.startsWith("http")) {
            return ref;
        }
        return ref.startsWith("/") ? baseUrl + ref : baseUrl + "/" + ref;
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    public static LocalDateTime openEndedEnd() {
        return endOfDay(OPEN_ENDED_DATE);
    }

    public static CreateEvent event(String title, String url, String img,
                                    LocalDateTime startDate, LocalDateTime endDate,
                                    Long brandId, boolean isActive) {
        return new CreateEvent(title, null, url, img, startDate, endDate, brandId, isActive, null);
    }
}
