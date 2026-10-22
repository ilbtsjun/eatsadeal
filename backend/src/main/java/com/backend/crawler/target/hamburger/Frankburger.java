package com.backend.crawler.target.hamburger;

import com.backend.brand.entity.Brand;
import com.backend.crawler.common.CrawlErrors;
import com.backend.crawler.common.CrawlHttpClient;
import com.backend.crawler.common.CrawlResult;
import com.backend.crawler.common.CrawlerUtil;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class Frankburger implements Crawler {

    private static final String BASE_URL = "https://frankburger.co.kr";
    private static final String EVENT_LIST_URL = BASE_URL + "/board/index.php?board=event_01&tab=3&type=list";

    private static final int MAX_PAGES = 20;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<li[^>]*>(.*?)</li>",
            Pattern.DOTALL
    );

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "href=[\"']([^\"']*board=event_01[^\"']*idx=(\\d+)[^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IDX_PATTERN = Pattern.compile(
            "[?&]idx=(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<[^>]*class=[\"'][^\"']*(?:subject|title)[^\"']*[\"'][^>]*>(.*?)</[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{4}/\\d{2}/\\d{2})(?:\\s*~\\s*(\\d{4}/\\d{2}/\\d{2})?)?"
    );

    private static final Pattern IMG_PATTERN = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern STYLE_BG_PATTERN = Pattern.compile(
            "background(?:-image)?\\s*:\\s*url\\(['\"]?([^'\")]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final CrawlHttpClient http;

    @Override
    public String getName() {
        return "Frankburger";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();
        Set<String> seenIds = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            try {
                String html = requestEventList(page);
                List<CreateEvent> pageEvents = parseEvents(html, brand.getId(), seenIds, errors);

                if (pageEvents.isEmpty()) {
                    break;
                }

                eventList.addAll(pageEvents);

                log.info("[프랭크버거] page={}, 이벤트 개수={}, 누적={}",
                        page, pageEvents.size(), eventList.size());
            } catch (Exception e) {
                log.error("[프랭크버거] 이벤트 목록 조회 오류(page={}): {}",
                        page, e.toString());
                errors.add("목록 조회 page=" + page, e);
                break;
            }
        }

        log.info("[프랭크버거] 총 이벤트 개수: {}", eventList.size());
        return CrawlResult.of(eventList, errors);
    }

    private String requestEventList(int page) throws Exception {
        return http.getString(EVENT_LIST_URL + "&page=" + page, StandardCharsets.UTF_8,
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }

    private List<CreateEvent> parseEvents(String html, Long brandId, Set<String> seenIds, CrawlErrors errors) {
        List<CreateEvent> result = new ArrayList<>();

        Matcher itemMatcher = ITEM_PATTERN.matcher(html);

        while (itemMatcher.find()) {
            String itemHtml = itemMatcher.group(1);

            Matcher linkMatcher = LINK_PATTERN.matcher(itemHtml);
            if (!linkMatcher.find()) {
                continue;
            }

            String href = linkMatcher.group(1);
            String eventId = linkMatcher.group(2);

            if (!seenIds.add(eventId)) {
                continue;
            }

            try {
                result.add(toCreateEvent(href, itemHtml, brandId));
            } catch (Exception e) {
                log.error("[프랭크버거] 개별 이벤트 파싱 오류(idx={}): {}",
                        eventId, e.toString());
                errors.add("이벤트 파싱 idx=" + eventId, e);
            }
        }

        return result;
    }

    private CreateEvent toCreateEvent(String href, String itemHtml, Long brandId) {
        String linkUrl = CrawlerUtil.absoluteUrl(BASE_URL, href);

        String title = extractTitle(itemHtml);
        String imgUrl = extractImage(itemHtml);

        Matcher dateMatcher = DATE_PATTERN.matcher(CrawlerUtil.cleanText(itemHtml));
        if (!dateMatcher.find()) {
            throw new IllegalArgumentException("시작일 파싱 실패");
        }

        LocalDateTime startDate = CrawlerUtil.startOfDay(LocalDate.parse(dateMatcher.group(1), DATE_FORMAT));
        LocalDateTime endDate = dateMatcher.group(2) == null
                ? CrawlerUtil.openEndedEnd()
                : CrawlerUtil.endOfDay(LocalDate.parse(dateMatcher.group(2), DATE_FORMAT));

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId,
                endDate.isAfter(LocalDateTime.now()));
    }

    private String extractTitle(String itemHtml) {
        Matcher titleMatcher = TITLE_PATTERN.matcher(itemHtml);

        if (titleMatcher.find()) {
            return CrawlerUtil.cleanText(titleMatcher.group(1));
        }

        String text = CrawlerUtil.cleanText(itemHtml);
        Matcher dateMatcher = DATE_PATTERN.matcher(text);

        if (dateMatcher.find()) {
            return text.substring(0, dateMatcher.start()).trim();
        }

        throw new IllegalArgumentException("제목 파싱 실패");
    }

    private String extractImage(String itemHtml) {
        Matcher styleMatcher = STYLE_BG_PATTERN.matcher(itemHtml);

        if (styleMatcher.find()) {
            return CrawlerUtil.absoluteUrl(BASE_URL, styleMatcher.group(1));
        }

        Matcher imgMatcher = IMG_PATTERN.matcher(itemHtml);

        if (imgMatcher.find()) {
            return CrawlerUtil.absoluteUrl(BASE_URL, imgMatcher.group(1));
        }

        return "";
    }
}
