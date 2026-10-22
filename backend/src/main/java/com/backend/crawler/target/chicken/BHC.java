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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BHC implements Crawler {
    private static final String API_URL = "https://www.bhc.co.kr/api/v1/web/events/list";

    private static final String ACTIVE_DETAIL_URL_PREFIX = "https://www.bhc.co.kr/event/currentEvent/";
    private static final String ENDED_LIST_URL = "https://www.bhc.co.kr/event/endedEvent/";

    private final CrawlHttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BHC";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();

        try {
            JsonNode root = objectMapper.readTree(http.getString(API_URL, "Accept", "application/json"));
            // 응답 형식: {"status":"success","code":200,"body":[ {...}, ... ]}
            for (JsonNode node : root.path("body")) {
                try {
                    eventList.add(toCreateEvent(node, brand.getId()));
                } catch (Exception e) {
                    log.error("[BHC] 개별 항목 파싱 오류: {}", e.toString());
                    errors.add("항목 파싱", e);
                }
            }

            log.info("[BHC] 크롤링된 이벤트 개수: {}", eventList.size());
        } catch (Exception e) {
            log.error("[BHC] 크롤링 실패: {}", e.toString());
            errors.add("크롤링", e);
        }

        return CrawlResult.of(eventList, errors);
    }

    private CreateEvent toCreateEvent(JsonNode node, Long brandId) {
        String title = node.get("eventNm").asText().trim();
        String imgUrl = node.get("thumbnail").asText();
        boolean isActive = "ACTIVE".equalsIgnoreCase(node.get("eventStatus").asText());
        String linkUrl = isActive
                ? ACTIVE_DETAIL_URL_PREFIX + node.get("eventIdx").asText()
                : ENDED_LIST_URL + node.get("eventIdx").asText();

        LocalDateTime startDate = LocalDateTime.parse(node.get("startDate").asText());
        LocalDateTime endDate = LocalDateTime.parse(node.get("endDate").asText());

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId, isActive);
    }
}