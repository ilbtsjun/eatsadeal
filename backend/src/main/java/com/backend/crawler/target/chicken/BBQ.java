package com.backend.crawler.target.chicken;

import com.backend.brand.entity.Brand;
import com.backend.crawler.common.CrawlErrors;
import com.backend.crawler.common.CrawlHttpClient;
import com.backend.crawler.common.CrawlResult;
import com.backend.crawler.common.CrawlerUtil;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BBQ implements Crawler {
    private static final String API_URL = "https://bbq.co.kr/api/delivery/content/event";
    private static final int PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SEARCH_TYPE_ONGOING = "OPEN";
    private static final String SEARCH_TYPE_CLOSED = "CLOSED";
    private final CrawlHttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BBQ";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();
        eventList.addAll(fetchAll(SEARCH_TYPE_ONGOING, true, brand.getId(), errors));
        eventList.addAll(fetchAll(SEARCH_TYPE_CLOSED, false, brand.getId(), errors));
        return CrawlResult.of(eventList, errors);
    }

    private List<CreateEvent> fetchAll(String searchType, boolean isOngoing, Long brandId, CrawlErrors errors) {
        List<CreateEvent> result = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            try {
                String url = API_URL + "?searchType=" + searchType + "&page=" + page + "&size=" + PAGE_SIZE;
                JsonNode root = objectMapper.readTree(http.getString(url, "Accept", "application/json"));
                for (JsonNode node : root.get("content")) {
                    try {
                        result.add(toCreateEvent(node, isOngoing, brandId));
                    } catch (Exception e) {
                        log.error("[BBQ] 개별 항목 파싱 오류: {}", e.toString());
                        errors.add("항목 파싱", e);
                    }
                }

                last = root.path("last").asBoolean(true);
                page++;
            } catch (Exception e) {
                log.error("[BBQ] {} 목록 조회 오류(page={}): {}", searchType, page, e.toString());
                errors.add(searchType + " 목록 조회", e);
                break;
            }
        }

        log.info("[BBQ] searchType={} 이벤트 개수: {}", searchType, result.size());
        return result;
    }

    private CreateEvent toCreateEvent(JsonNode node, boolean isOngoing, Long brandId) {
        String title = node.get("title").asText();
        String linkUrl = "https://bbq.co.kr/events/" + node.get("id").asText();
        String imgUrl = node.get("thumbnailImageUrl").asText();


        LocalDateTime startDate = LocalDateTime.parse(node.get("startsAt").asText(), DATE_TIME_FORMAT);
        LocalDateTime endDate = LocalDateTime.parse(node.get("endsAt").asText(), DATE_TIME_FORMAT);

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId, isOngoing);
    }
}