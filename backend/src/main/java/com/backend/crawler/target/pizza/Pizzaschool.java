package com.backend.crawler.target.pizza;

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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class Pizzaschool implements Crawler {
    private static final String BASE_URL = "http://pizzaschool.net";
    private static final String LIST_URL = BASE_URL + "/%EC%9D%B4%EB%B2%A4%ED%8A%B8/";
    private static final int PAGES_TO_FETCH = 50;

    private static final Pattern TIME_PATTERN =
            Pattern.compile("<time[^>]*datetime=['\"](\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_DATE_PATTERN = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");

    private static final Pattern ARTICLE_PATTERN =
            Pattern.compile("<article class=['\"]slide-entry[^'\"]*['\"][^>]*>(.*?)</article>", Pattern.DOTALL);
    private static final Pattern HREF_PATTERN =
            Pattern.compile("<a href=['\"]([^'\"]+)['\"] data-rel=['\"]slide-1['\"] class=['\"]slide-image['\"]");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*\\bsrc=['\"]([^'\"]+)['\"]");
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<h3 class=['\"]slide-entry-title entry-title[^'\"]*['\"][^>]*><a[^>]*>([^<]+)</a></h3>");

    private final CrawlHttpClient http;

    @Override
    public String getName() {
        return "PizzaSchool";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> result = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();

        for (int page = 1; page <= PAGES_TO_FETCH; page++) {
            String url = page == 1 ? LIST_URL : LIST_URL + "?avia-element-paging=" + page;

            String html;
            try {
                html = http.getString(url);
            } catch (Exception e) {
                log.error("[피자스쿨] 목록 조회 오류(url={}): {}", url, e.toString());
                errors.add("목록 조회 url=" + url, e);
                break;
            }

            Matcher articleMatcher = ARTICLE_PATTERN.matcher(html);
            while (articleMatcher.find()) {
                String articleHtml = articleMatcher.group(1);
                try {
                    result.add(toCreateEvent(articleHtml, brand.getId()));
                } catch (Exception e) {
                    log.error("[피자스쿨] 개별 항목 파싱 오류: {}", e.toString());
                    errors.add("항목 파싱", e);
                }
            }
        }

        log.info("[피자스쿨] 이벤트 개수: {}", result.size());
        return CrawlResult.of(result, errors);
    }

    private CreateEvent toCreateEvent(String articleHtml, Long brandId) {
        Matcher hrefMatcher = HREF_PATTERN.matcher(articleHtml);
        if (!hrefMatcher.find()) {
            throw new IllegalArgumentException("링크 파싱 실패");
        }
        String linkUrl = hrefMatcher.group(1);

        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(articleHtml);
        String imgUrl = imgMatcher.find() ? imgMatcher.group(1) : "";

        Matcher titleMatcher = TITLE_PATTERN.matcher(articleHtml);
        String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "";
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }

        LocalDate posted = postedDate(articleHtml, title);
        if (posted == null) {
            return CrawlerUtil.event(title, linkUrl, imgUrl, null, null, brandId, true);
        }
        LocalDateTime startDate = CrawlerUtil.startOfDay(posted);
        LocalDateTime endDate = CrawlerUtil.endOfDay(posted.plusDays(CreateEvent.DEFAULT_PERIOD_DAYS));

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId,
                endDate.isAfter(LocalDateTime.now()));
    }

    private LocalDate postedDate(String articleHtml, String title) {
        Matcher timeMatcher = TIME_PATTERN.matcher(articleHtml);
        if (timeMatcher.find()) {
            return LocalDate.parse(timeMatcher.group(1));
        }

        Matcher titleMatcher = TITLE_DATE_PATTERN.matcher(title);
        if (titleMatcher.find()) {
            LocalDate today = LocalDate.now();
            try {
                LocalDate date = LocalDate.of(today.getYear(),
                        Integer.parseInt(titleMatcher.group(1)), Integer.parseInt(titleMatcher.group(2)));
                return date.isAfter(today.plusDays(7)) ? date.minusYears(1) : date;
            } catch (DateTimeException e) {
                return null;
            }
        }
        return null;
    }
}