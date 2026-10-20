package com.backend.crawler.target.chicken;

import com.backend.brand.entity.Brand;
import com.backend.crawler.common.CrawlHttpClient;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class Pelicana implements Crawler {
    private static final String LIST_API_URL = "https://www.pelicana.co.kr/api/common/event";
    private static final String DETAIL_API_URL = "https://www.pelicana.co.kr/api/common/event?event_seq=";
    private static final String IMG_BASE_URL = "https://www.pelicana.co.kr";
    private static final String DETAIL_URL_PREFIX = "https://www.pelicana.co.kr/community/event_detail?id=";
    private static final int CONTENT_SIZE = 20;

    private final CrawlHttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "Pelicana";
    }

    @Override
    public List<CreateEvent> crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();

        for (long eventSeq : fetchAllEventSeqs()) {
            try {
                CreateEvent event = fetchDetail(eventSeq, brand.getId());
                if (event != null) {
                    eventList.add(event);
                }
            } catch (Exception e) {
                log.error("[Pelicana] 상세 조회 오류(eventSeq={}): {}", eventSeq, e.getMessage());
            }
        }

        log.info("[Pelicana] 이벤트 개수: {}", eventList.size());
        return eventList;
    }

    private List<Long> fetchAllEventSeqs() {
        List<Long> seqList = new ArrayList<>();
        int pageNum = 0;
        boolean last = false;

        while (!last) {
            try {
                String requestBody = "{\"contentSize\":" + CONTENT_SIZE + ",\"pageNum\":" + pageNum + "}";
                HttpRequest request = HttpRequest.newBuilder(URI.create(LIST_API_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("[Pelicana] 목록 조회 실패(pageNum={}): status={}", pageNum, response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultData = root.path("result_data");

                for (JsonNode node : resultData.path("content")) {
                    seqList.add(node.get("eventSeq").asLong());
                }

                last = resultData.path("last").asBoolean(true);
                pageNum++;
            } catch (Exception e) {
                log.error("[Pelicana] 목록 조회 오류(pageNum={}): {}", pageNum, e.getMessage());
                break;
            }
        }

        return seqList;
    }

    private CreateEvent fetchDetail(long eventSeq, Long brandId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(DETAIL_API_URL + eventSeq))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[Pelicana] 상세 조회 실패(eventSeq={}): status={}", eventSeq, response.statusCode());
            return null;
        }

        JsonNode node = objectMapper.readTree(response.body()).path("result_data");

        String title = node.get("eventTitle").asText().trim();
        String linkUrl = DETAIL_URL_PREFIX + eventSeq;
        String imgUrl = IMG_BASE_URL + node.get("listImgFp").asText();

        String startDateStr = node.path("openStartDt").asText("");
        String endDateStr = node.path("openEndDt").asText("");

        LocalDateTime startDate;
        LocalDateTime endDate;
        boolean isActive;

        if (startDateStr.isBlank() || endDateStr.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            startDate = now;
            endDate = now;
            isActive = false;
        } else {
            LocalDate start = LocalDate.parse(startDateStr);
            LocalDate end = LocalDate.parse(endDateStr);
            LocalDate today = LocalDate.now();

            startDate = start.atStartOfDay();
            endDate = end.atTime(23, 59, 59);
            isActive = !today.isBefore(start) && !today.isAfter(end);
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
                null);
    }
}