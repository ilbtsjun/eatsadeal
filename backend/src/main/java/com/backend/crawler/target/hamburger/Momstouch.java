package com.backend.crawler.target.hamburger;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
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
public class Momstouch implements Crawler {

    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://momstouch.co.kr";
    private static final String EVENT_LIST_URL = BASE_URL + "/promotion/inner_promotion_list.php";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(2099, 12, 31);

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<li[^>]*>(.*?)</li>",
            Pattern.DOTALL
    );

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "go_view\\(['\"](\\d+)['\"]\\)",
            Pattern.DOTALL
    );

    private static final Pattern IMG_PATTERN = Pattern.compile(
            "background-image\\s*:\\s*url\\(['\"]?([^'\")]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<h4[^>]*class=\"[^\"]*title[^\"]*\"[^>]*>(.*?)</h4>",
            Pattern.DOTALL
    );

    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "<p[^>]*class=\"[^\"]*date[^\"]*\"[^>]*>(.*?)</p>",
            Pattern.DOTALL
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "Momstouch";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        Long brandId = brandRepository.findByName(getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, getName() + " 브랜드가 DB에 없습니다."))
                .getId();

        eventList.addAll(fetchAll("Y", true, brandId));
        eventList.addAll(fetchAll("N", false, brandId));

        log.info("[맘스터치] 총 이벤트 개수: {}", eventList.size());
        return eventList;
    }

    private List<CreateEvent> fetchAll(String status, boolean isActive, Long brandId) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String html;

            try {
                html = requestEventList(page, status);
            } catch (Exception e) {
                log.error("[맘스터치] 이벤트 목록 조회 오류(status={}, page={}): {}",
                        status, page, e.getMessage());
                break;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(html);
            boolean foundNew = false;

            while (itemMatcher.find()) {
                String itemHtml = itemMatcher.group(1);

                Matcher linkMatcher = LINK_PATTERN.matcher(itemHtml);
                if (!linkMatcher.find()) {
                    continue;
                }

                String eventId = linkMatcher.group(1);
                String href = "/promotion/view.php?idx=" + eventId + "&pageNo=" + page;
                String linkUrl = BASE_URL + href;

                if (!seenUrls.add(linkUrl)) {
                    continue;
                }

                foundNew = true;

                try {
                    result.add(toCreateEvent(href, itemHtml, isActive, brandId));
                } catch (Exception e) {
                    log.error("[맘스터치] 개별 이벤트 파싱 오류(url={}): {}",
                            linkUrl, e.getMessage());
                }
            }

            log.info("[맘스터치] status={}, page={}, 이벤트 개수={}",
                    status, page, result.size());

            if (!foundNew) {
                break;
            }
        }

        return result;
    }

    private String requestEventList(int page, String status) throws Exception {
        String requestBody = "pageNo=" + page;

        String eventListUrl = EVENT_LIST_URL
                + "?field=&keyword=&v_sect=&s_gubun=&s_level=" + status;

        HttpRequest request = HttpRequest.newBuilder(URI.create(eventListUrl))
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody,
                        StandardCharsets.UTF_8
                ))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Origin", BASE_URL)
                .header("Referer", BASE_URL + "/promotion/list.php")
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "HTTP status=" + response.statusCode() + ", body=" + response.body()
            );
        }

        return response.body();
    }

    private CreateEvent toCreateEvent(String href, String itemHtml, boolean isActive, Long brandId) {
        String linkUrl = BASE_URL + href;

        Matcher imgMatcher = IMG_PATTERN.matcher(itemHtml);
        String imgUrl = imgMatcher.find() ? imgMatcher.group(1) : "";

        if (imgUrl.startsWith("/")) {
            imgUrl = BASE_URL + imgUrl;
        }

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

        Matcher dateMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(period);

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