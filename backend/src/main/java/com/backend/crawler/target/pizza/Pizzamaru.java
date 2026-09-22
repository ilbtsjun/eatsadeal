package com.backend.crawler.target.pizza;

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
public class Pizzamaru implements Crawler {
    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://www.pizzamaru.co.kr";
    private static final String ONGOING_URL = BASE_URL + "/event/01/";
    private static final String ENDED_URL = BASE_URL + "/event/01/?endchk=1";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50; // 무한 루프 방지 안전장치
    private static final String PAGE_PARAM = "page";

    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<li>\\s*<a href=\"([^\"]+)\">(.*?)</a>\\s*</li>", Pattern.DOTALL);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*src=\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<p class=\"fz18 fw5\">([^<]*)</p>");
    private static final Pattern DATE_LINE_PATTERN = Pattern.compile("<p class=\"c6\">([^<]*)</p>");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "PizzaMaru";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        eventList.addAll(fetchAll(ONGOING_URL, true));
        eventList.addAll(fetchAll(ENDED_URL, false));
        return eventList;
    }

    private List<CreateEvent> fetchAll(String baseUrl, boolean isOngoing) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();
        String joinChar = baseUrl.contains("?") ? "&" : "?";

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = page == 1 ? baseUrl : baseUrl + joinChar + PAGE_PARAM + "=" + page;

            String html;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("[피자마루] 목록 조회 실패(url={}): status={}", url, response.statusCode());
                    break;
                }
                html = response.body();
            } catch (Exception e) {
                log.error("[피자마루] 목록 조회 오류(url={}): {}", url, e.getMessage());
                break;
            }

            Matcher cardMatcher = CARD_PATTERN.matcher(html);
            boolean foundNew = false;

            while (cardMatcher.find()) {
                String href = cardMatcher.group(1);
                if (!seenHrefs.add(href)) {
                    continue;
                }
                foundNew = true;
                try {
                    result.add(toCreateEvent(href, cardMatcher.group(2), isOngoing));
                } catch (Exception e) {
                    log.error("[피자마루] 개별 항목 파싱 오류(href={}): {}", href, e.getMessage());
                }
            }

            if (!foundNew) {
                break; // 등록된 게시물이 없거나 마지막 페이지
            }
        }

        log.info("[피자마루] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private CreateEvent toCreateEvent(String href, String cardHtml, boolean isOngoing) {
        String linkUrl = href.startsWith("http") ? href : BASE_URL + href;

        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(cardHtml);
        String imgSrc = imgMatcher.find() ? imgMatcher.group(1) : "";
        String imgUrl = imgSrc.startsWith("http") ? imgSrc : BASE_URL + imgSrc;

        Matcher titleMatcher = TITLE_PATTERN.matcher(cardHtml);
        String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "";
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }

        Matcher dateLineMatcher = DATE_LINE_PATTERN.matcher(cardHtml);
        if (!dateLineMatcher.find()) {
            throw new IllegalArgumentException("기간 파싱 실패");
        }
        Matcher dateMatcher = DATE_PATTERN.matcher(dateLineMatcher.group(1));
        String startDateStr = dateMatcher.find() ? dateMatcher.group() : null;
        String endDateStr = dateMatcher.find() ? dateMatcher.group() : null;
        if (startDateStr == null || endDateStr == null) {
            throw new IllegalArgumentException("기간 파싱 실패: " + dateLineMatcher.group(1));
        }

        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);

        Long brandId = brandRepository.findByName(getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, getName() + " 브랜드가 DB에 없습니다."))
                .getId();

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imgUrl,
                startDate,
                endDate,
                brandId,
                isOngoing,
                null);
    }
}