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
import java.time.LocalDateTime;
import java.time.YearMonth;
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
public class Pizzaetang implements Crawler {
    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://pizzaetang.com";
    private static final String LIST_URL =
            BASE_URL + "/shop1/front/php/b/board_list.php?board_no=12&is_pcver=T";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50; // 무한 루프 방지 안전장치
    private static final DateTimeFormatter REG_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<a href=\"(/article/promotion/12/\\d+/)\">(.*?)</a>", Pattern.DOTALL);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img src=\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<strong class=\"subject[^\"]*\">([^<]+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("<span class=\"date[^\"]*\">([^<]*)</span>");
    // 제목의 "N월"에서 월 숫자를 뽑아내기 위한 패턴 (예: "9월 할인 프로모션" -> 9)
    private static final Pattern MONTH_IN_TITLE_PATTERN = Pattern.compile("(\\d{1,2})월");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "Pizzaetang";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();

        String url = LIST_URL;

        String html = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("User-Agent", USER_AGENT)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("[피자에땅] 목록 조회 실패(url={}): status={}", url, response.statusCode());
            }
            html = response.body();
        } catch (Exception e) {
            log.error("[피자에땅] 목록 조회 오류(url={}): {}", url, e.getMessage());
        }

        Matcher cardMatcher = CARD_PATTERN.matcher(html);

        while (cardMatcher.find()) {
            String href = cardMatcher.group(1);
            String cardHtml = cardMatcher.group(2);
            if (!seenHrefs.add(href)) {
                continue; // 이미 수집한 항목 (마지막 페이지를 넘어가서 반복되는 경우)
            }
            try {
                result.add(toCreateEvent(href, cardHtml));
            } catch (Exception e) {
                log.error("[피자에땅] 개별 항목 파싱 오류(href={}): {}", href, e.getMessage());
            }
        }

        log.info("[피자에땅] 이벤트 개수: {}", result.size());
        return result;
    }

    private CreateEvent toCreateEvent(String href, String cardHtml) {
        String linkUrl = href.startsWith("http") ? href : BASE_URL + href;

        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(cardHtml);
        String imgSrc = imgMatcher.find() ? imgMatcher.group(1) : "";
        String imgUrl = imgSrc.startsWith("//") ? "https:" + imgSrc
                : imgSrc.startsWith("http") ? imgSrc : BASE_URL + imgSrc;

        Matcher titleMatcher = TITLE_PATTERN.matcher(cardHtml);
        String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "";
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(cardHtml);
        if (!dateMatcher.find()) {
            throw new IllegalArgumentException("등록일 파싱 실패");
        }
        LocalDateTime regDateTime = LocalDateTime.parse(dateMatcher.group(1).trim(), REG_DATE_FORMAT);
        Matcher monthMatcher = MONTH_IN_TITLE_PATTERN.matcher(title);
        YearMonth yearMonth;
        if (monthMatcher.find()) {
            int month = Integer.parseInt(monthMatcher.group(1));
            yearMonth = YearMonth.of(regDateTime.getYear(), month);
        } else {
            yearMonth = YearMonth.of(regDateTime.getYear(), regDateTime.getMonthValue());
        }

        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime now = LocalDateTime.now();
        boolean isActive = !now.isBefore(startDate) && !now.isAfter(endDate);

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
                isActive,
                null);
    }
}