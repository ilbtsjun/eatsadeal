package com.backend.crawler.target.hamburger;

import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import com.backend.brand.repository.BrandRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class KFC implements Crawler {

    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://www.kfckorea.com";
    private static final String ONGOING_URL = BASE_URL + "/promotion/promotionList/A801";
    private static final String ENDED_URL = BASE_URL + "/promotion/promotionEndList";

    private static final String DETAIL_URL = BASE_URL + "/promotion/promotionlist/detail/";
    private static final String ENDED_DETAIL_URL = BASE_URL + "/promotion/promotionEndList/detail/";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern INITIAL_STATE_PATTERN = Pattern.compile(
            "window\\.__INITIAL_COMPONENTS_STATE__\\s*=\\s*(\\[.*?\\])\\s*;",
            Pattern.DOTALL
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String getName() {
        return "KFC";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> events = new ArrayList<>();

        try {
            Long brandId = brandRepository.findByName(getName())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, getName() + " 브랜드가 DB에 없습니다."))
                    .getId();

            events.addAll(crawlList(ONGOING_URL, brandId, true));
            events.addAll(crawlList(ENDED_URL, brandId, false));
        } catch (Exception e) {
            log.error("KFC 이벤트 크롤링 실패", e);
        }

        return events;
    }

    private List<CreateEvent> crawlList(String url, Long brandId, boolean isActive) {
        List<CreateEvent> events = new ArrayList<>();

        try {
            String html = request(url);
            Matcher matcher = INITIAL_STATE_PATTERN.matcher(html);

            if (!matcher.find()) {
                log.warn("KFC 초기 상태 데이터를 찾을 수 없습니다. url={}", url);
                return events;
            }

            JsonNode state = OBJECT_MAPPER.readTree(matcher.group(1));

            for (JsonNode node : state) {
                if (node == null || !node.has("listData")) {
                    continue;
                }

                JsonNode listData = node.get("listData");
                JsonNode rows = listData.get("rows");

                if (rows == null || !rows.isArray()) {
                    continue;
                }

                for (JsonNode row : rows) {
                    CreateEvent event = toCreateEvent(row, brandId, isActive);

                    if (event != null) {
                        events.add(event);
                    }
                }

                break;
            }
        } catch (Exception e) {
            log.error("KFC 이벤트 목록 크롤링 실패. url={}", url, e);
        }

        return events;
    }

    private CreateEvent toCreateEvent(JsonNode row, Long brandId, boolean isActive) {
        try {
            String eventIndex = getText(row, "event_index");
            String title = getText(row, "event_title");

            if (eventIndex == null || title == null) {
                return null;
            }

            String startDateText = getText(row, "event_show_str_date");
            String endDateText = getText(row, "event_show_end_date");

            LocalDateTime startDate = parseDate(startDateText);
            LocalDateTime endDate = parseDate(endDateText);

            if (startDate == null) {
                startDate = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
            }

            if (endDate == null || endDate.getYear() >= 9999) {
                endDate = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            }

            String imgUrl = getText(row, "event_web_list_img");

            if (imgUrl == null || imgUrl.isBlank()) {
                imgUrl = getText(row, "event_web_img");
            }

            if (imgUrl != null && imgUrl.startsWith("/")) {
                imgUrl = BASE_URL + imgUrl;
            }

            String linkUrl = (isActive ? DETAIL_URL : ENDED_DETAIL_URL) + eventIndex;

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
        } catch (Exception e) {
            log.error("KFC 이벤트 변환 실패. row={}", row, e);
            return null;
        }
    }

    private String request(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();

        return text == null || text.isBlank() ? null : text.trim();
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            if (date.length() == 10) {
                return LocalDateTime.parse(date + " 00:00:00", DATE_FORMATTER);
            }

            return LocalDateTime.parse(date, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("KFC 날짜 파싱 실패. date={}", date);
            return null;
        }
    }
}