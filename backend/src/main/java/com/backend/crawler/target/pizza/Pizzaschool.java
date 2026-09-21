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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class Pizzaschool implements Crawler {
    private final BrandRepository brandRepository;

    private static final String BASE_URL = "http://pizzaschool.net";
    private static final String LIST_URL = BASE_URL + "/%EC%9D%B4%EB%B2%A4%ED%8A%B8/";
    private static final int PAGES_TO_FETCH = 50;

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final Pattern ARTICLE_PATTERN =
            Pattern.compile("<article class=['\"]slide-entry[^'\"]*['\"][^>]*>(.*?)</article>", Pattern.DOTALL);
    private static final Pattern HREF_PATTERN =
            Pattern.compile("<a href=['\"]([^'\"]+)['\"] data-rel=['\"]slide-1['\"] class=['\"]slide-image['\"]");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*\\bsrc=['\"]([^'\"]+)['\"]");
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<h3 class=['\"]slide-entry-title entry-title[^'\"]*['\"][^>]*><a[^>]*>([^<]+)</a></h3>");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "PizzaSchool";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> result = new ArrayList<>();

        for (int page = 1; page <= PAGES_TO_FETCH; page++) {
            String url = page == 1 ? LIST_URL : LIST_URL + "?avia-element-paging=" + page;

            String html;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("[피자스쿨] 목록 조회 실패(url={}): status={}", url, response.statusCode());
                    break;
                }
                html = response.body();
            } catch (Exception e) {
                log.error("[피자스쿨] 목록 조회 오류(url={}): {}", url, e.getMessage());
                break;
            }

            Matcher articleMatcher = ARTICLE_PATTERN.matcher(html);
            while (articleMatcher.find()) {
                String articleHtml = articleMatcher.group(1);
                try {
                    result.add(toCreateEvent(articleHtml));
                } catch (Exception e) {
                    log.error("[피자스쿨] 개별 항목 파싱 오류: {}", e.getMessage());
                }
            }
        }

        log.info("[피자스쿨] 이벤트 개수: {}", result.size());
        return result;
    }

    private CreateEvent toCreateEvent(String articleHtml) {
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
        LocalDateTime now = LocalDateTime.now();

        Long brandId = brandRepository.findByName(getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, getName() + " 브랜드가 DB에 없습니다."))
                .getId();

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imgUrl,
                now,
                now,
                brandId,
                true,
                null);
    }
}