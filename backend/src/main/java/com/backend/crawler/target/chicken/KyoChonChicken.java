package com.backend.crawler.target.chicken;

import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import com.backend.brand.repository.BrandRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class KyoChonChicken implements Crawler {
    private final BrandRepository brandRepository;

    private static final String DIR_URL = "https://www.kyochon.com/event/";
    private static final String ONGOING_URL = DIR_URL + "ing.asp";
    private static final String ENDED_URL = DIR_URL + "end.asp";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 200;

    private static final Pattern LIST_BLOCK_PATTERN =
            Pattern.compile("<ul class=\"eventList[^\"]*\">(.*?)</ul>", Pattern.DOTALL);
    private static final Pattern ITEM_PATTERN = Pattern.compile("<li>(.*?)</li>", Pattern.DOTALL);
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*src=\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<dt><a[^>]*>(.*?)</a></dt>", Pattern.DOTALL);
    private static final Pattern PERIOD_PATTERN = Pattern.compile("<dd>(.*?)</dd>", Pattern.DOTALL);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final HttpClient httpClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

    @Override
    public String getName() {
        return "KyoChonChicken";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        eventList.addAll(fetchAll(ONGOING_URL, true));
        eventList.addAll(fetchAll(ENDED_URL, false));
        return eventList;
    }

    private List<CreateEvent> fetchAll(String listUrl, boolean isOngoing) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();

        for (int pageNum = 1; pageNum <= MAX_PAGES; pageNum++) {
            String url = pageNum == 1 ? listUrl : listUrl + "?page=" + pageNum;

            String html;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("[교촌] 목록 조회 실패(url={}): status={}", url, response.statusCode());
                    break;
                }
                html = response.body();
            } catch (Exception e) {
                log.error("[교촌] 목록 조회 오류(url={}): {}", url, e.getMessage());
                break;
            }

            Matcher listMatcher = LIST_BLOCK_PATTERN.matcher(html);
            if (!listMatcher.find()) {
                break;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(listMatcher.group(1));
            boolean foundAny = false;
            boolean foundNew = false;

            while (itemMatcher.find()) {
                foundAny = true;
                String itemHtml = itemMatcher.group(1);
                try {
                    Matcher hrefMatcher = HREF_PATTERN.matcher(itemHtml);
                    if (!hrefMatcher.find()) {
                        continue;
                    }
                    String linkUrl = DIR_URL + hrefMatcher.group(1);
                    if (!seenHrefs.add(linkUrl)) {
                        continue;
                    }
                    foundNew = true;
                    result.add(toCreateEvent(itemHtml, linkUrl, isOngoing));
                } catch (Exception e) {
                    log.error("[교촌] 개별 항목 파싱 오류: {}", e.getMessage());
                }
            }

            if (!foundAny || !foundNew) {
                break;
            }
        }

        log.info("[교촌] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private CreateEvent toCreateEvent(String itemHtml, String linkUrl, boolean isOngoing) {
        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(itemHtml);
        Matcher titleMatcher = TITLE_PATTERN.matcher(itemHtml);
        Matcher periodMatcher = PERIOD_PATTERN.matcher(itemHtml);

        if (!imgMatcher.find() || !titleMatcher.find() || !periodMatcher.find()) {
            throw new IllegalArgumentException("필수 요소 누락");
        }

        String imgUrl = "https://www.kyochon.com" + imgMatcher.group(1);
        String title = titleMatcher.group(1).trim();
        String periodText = periodMatcher.group(1).trim();

        Matcher dateMatcher = DATE_PATTERN.matcher(periodText);
        String startDateStr = dateMatcher.find() ? dateMatcher.group() : null;
        String endDateStr = dateMatcher.find() ? dateMatcher.group() : null;

        if (title.isBlank() || startDateStr == null || endDateStr == null) {
            throw new IllegalArgumentException("제목/기간 파싱 실패: title=" + title + ", period=" + periodText);
        }

        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imgUrl,
                startDate,
                endDate,
                brandRepository.findByName(getName()).getId(),
                isOngoing,
                null);
    }
}