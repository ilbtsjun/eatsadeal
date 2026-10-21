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
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();

        for (long eventSeq : fetchAllEventSeqs(errors)) {
            try {
                eventList.add(fetchDetail(eventSeq, brand.getId()));
            } catch (Exception e) {
                log.error("[Pelicana] 상세 조회 오류(eventSeq={}): {}", eventSeq, e.toString());
                errors.add("상세 조회 eventSeq=" + eventSeq, e);
            }
        }

        log.info("[Pelicana] 이벤트 개수: {}", eventList.size());
        return CrawlResult.of(eventList, errors);
    }

    private List<Long> fetchAllEventSeqs(CrawlErrors errors) {
        List<Long> seqList = new ArrayList<>();
        int pageNum = 0;
        boolean last = false;

        while (!last) {
            try {
                String requestBody = "{\"contentSize\":" + CONTENT_SIZE + ",\"pageNum\":" + pageNum + "}";
                JsonNode root = objectMapper.readTree(
                        http.postString(LIST_API_URL, "application/json", requestBody, "Accept", "application/json"));
                JsonNode resultData = root.path("result_data");

                for (JsonNode node : resultData.path("content")) {
                    seqList.add(node.get("eventSeq").asLong());
                }

                last = resultData.path("last").asBoolean(true);
                pageNum++;
            } catch (Exception e) {
                log.error("[Pelicana] 목록 조회 오류(pageNum={}): {}", pageNum, e.toString());
                errors.add("목록 조회", e);
                break;
            }
        }

        return seqList;
    }

    private CreateEvent fetchDetail(long eventSeq, Long brandId) throws Exception {
        String body = http.getString(DETAIL_API_URL + eventSeq, "Accept", "application/json");
        JsonNode node = objectMapper.readTree(body).path("result_data");

        String title = node.get("eventTitle").asText().trim();
        String linkUrl = DETAIL_URL_PREFIX + eventSeq;
        String imgUrl = IMG_BASE_URL + node.get("listImgFp").asText();

        String startDateStr = node.path("openStartDt").asText("");
        String endDateStr = node.path("openEndDt").asText("");

        LocalDateTime startDate;
        LocalDateTime endDate;
        boolean isActive;

        if (startDateStr.isBlank() || endDateStr.isBlank()) {
            startDate = null;
            endDate = null;
            isActive = false;
        } else {
            LocalDate start = LocalDate.parse(startDateStr);
            LocalDate end = LocalDate.parse(endDateStr);
            LocalDate today = LocalDate.now();

            startDate = CrawlerUtil.startOfDay(start);
            endDate = CrawlerUtil.endOfDay(end);
            isActive = !today.isBefore(start) && !today.isAfter(end);
        }

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId, isActive);
    }
}