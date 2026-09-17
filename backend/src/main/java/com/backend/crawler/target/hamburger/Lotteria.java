package com.backend.crawler.target.hamburger;

import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import com.backend.brand.repository.BrandRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
public class Lotteria implements Crawler {

    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://www.lotteeatz.com";
    private static final String EVENT_LIST_API = BASE_URL + "/event/main/eventListAjax";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(2099, 12, 31);

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<li[^>]*class=\"[^\"]*grid-item[^\"]*\"[^>]*>(.*?)</li>",
            Pattern.DOTALL
    );

    private static final Pattern IMG_PATTERN = Pattern.compile(
            "<img[^>]*src=\"([^\"]+)\"",
            Pattern.DOTALL
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<div[^>]*class=\"[^\"]*grid-title[^\"]*\"[^>]*>(.*?)</div>",
            Pattern.DOTALL
    );

    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "<div[^>]*class=\"[^\"]*grid-period[^\"]*\"[^>]*>(.*?)</div>",
            Pattern.DOTALL
    );

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a[^>]*href=\"(/event/main/selectEvent/\\d+)\"",
            Pattern.DOTALL
    );

    private static final Pattern EVENT_ID_PATTERN = Pattern.compile(
            "id=\"a11y(\\d+)_\\d+\""
    );

    private static final Pattern BRAND_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*text[^\"]*\"[^>]*>\\s*롯데리아\\s*</span>",
            Pattern.DOTALL
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "Lotteria";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();

        Long brandId = brandRepository.findByName(getName()).getId();

        eventList.addAll(fetchAll("RUN", true, brandId));
        eventList.addAll(fetchAll("EXP", false, brandId));

        log.info("[롯데리아] 총 이벤트 개수: {}", eventList.size());
        return eventList;
    }

    private List<CreateEvent> fetchAll(String eventStatusCode, boolean isActive, Long brandId) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String html;
            try {
                html = requestEventList(page, eventStatusCode);
            } catch (Exception e) {
                log.error("[롯데리아] 이벤트 목록 조회 오류(status={}, page={}): {}", eventStatusCode, page, e.getMessage());
                break;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(html);
            boolean foundNew = false;

            while (itemMatcher.find()) {
                String itemHtml = itemMatcher.group(1);
                Matcher brandMatcher = BRAND_PATTERN.matcher(itemHtml);

                if (!brandMatcher.find()) {
                    continue;
                }

                String href = extractHref(itemHtml);

                if (href == null) {
                    log.warn("[롯데리아] 이벤트 상세 URL 파싱 실패");
                    continue;
                }

                if (!seenHrefs.add(href)) {
                    continue;
                }

                foundNew = true;

                try {
                    result.add(toCreateEvent(href, itemHtml, isActive, brandId));
                } catch (Exception e) {
                    log.error("[롯데리아] 개별 이벤트 파싱 오류(href={}): {}", href, e.getMessage());
                }
            }

            log.info("[롯데리아] status={}, page={}, 이벤트 개수={}", eventStatusCode, page, result.size());

            if (!foundNew) {
                break;
            }
        }

        return result;
    }

    private String extractHref(String itemHtml) {
        Matcher linkMatcher = LINK_PATTERN.matcher(itemHtml);

        if (linkMatcher.find()) {
            return linkMatcher.group(1);
        }

        Matcher eventIdMatcher = EVENT_ID_PATTERN.matcher(itemHtml);

        if (eventIdMatcher.find()) {
            return "/event/main/selectEvent/" + eventIdMatcher.group(1);
        }

        return null;
    }

    private String requestEventList(int page, String eventStatusCode) throws Exception {
        String requestBody = """
                {
                    "divcd": "10",
                    "page": %d,
                    "eventSttusCode": "%s"
                }
                """.formatted(page, eventStatusCode);

        HttpRequest request = HttpRequest.newBuilder(URI.create(EVENT_LIST_API))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Origin", BASE_URL)
                .header("Referer", BASE_URL + "/event/main")
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP status=" + response.statusCode() + ", body=" + response.body());
        }

        return response.body();
    }

    private CreateEvent toCreateEvent(String href, String itemHtml, boolean isActive, Long brandId) {
        String linkUrl = BASE_URL + href;

        Matcher imgMatcher = IMG_PATTERN.matcher(itemHtml);
        String imgUrl = imgMatcher.find() ? imgMatcher.group(1) : "";

        Matcher titleMatcher = TITLE_PATTERN.matcher(itemHtml);

        if (!titleMatcher.find()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }

        String title = cleanText(titleMatcher.group(1));

        Matcher periodMatcher = PERIOD_PATTERN.matcher(itemHtml);

        if (!periodMatcher.find()) {
            throw new IllegalArgumentException("기간 파싱 실패");
        }

        String period = cleanText(periodMatcher.group(1));

        Matcher dateMatcher = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})").matcher(period);

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (dateMatcher.find()) {
            startDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMAT).atStartOfDay();
        }

        if (dateMatcher.find()) {
            endDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMAT).atTime(23, 59, 59);
        } else {
            endDate = OPEN_ENDED_DATE.atTime(23, 59, 59);
        }

        if (startDate == null) {
            throw new IllegalArgumentException("시작일 파싱 실패");
        }

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imgUrl,
                startDate,
                endDate,
                brandId,
                isActive,
                null
        );
    }

    private String cleanText(String html) {
        return html
                .replaceAll("<[^>]*>", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}