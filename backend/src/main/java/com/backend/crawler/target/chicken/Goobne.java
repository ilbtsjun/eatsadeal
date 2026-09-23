package com.backend.crawler.target.chicken;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class Goobne implements Crawler {
    private final BrandRepository brandRepository;

    private static final String API_URL = "https://www.goobne.co.kr/brd/event/srch_list_p";
    private static final String IMG_BASE_URL = "https://www.goobne.co.kr";
    private static final String DETAIL_URL_PREFIX = "https://www.goobne.co.kr/brd/event/view_p?seq=";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int LIST_COUNT = 50; // 한 번에 최대한 크게 요청해서 페이지 요청 횟수를 줄임
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "Goobne";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        eventList.addAll(fetchAll("now_date", true));
        eventList.addAll(fetchAll("end_date", false));
        return eventList;
    }

    private List<CreateEvent> fetchAll(String eventGb, boolean isOngoing) {
        List<CreateEvent> result = new ArrayList<>();
        Long brandId = brandRepository.findByName(getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, getName() + " 브랜드가 DB에 없습니다."))
                .getId();
        int pageNum = 1;
        int totalCnt = Integer.MAX_VALUE;

        while (result.size() < totalCnt) {
            try {
                String requestBody = "{"
                        + "\"eventGb\":\"" + eventGb + "\","
                        + "\"use_yn\":\"Y\","
                        + "\"pageNum\":" + pageNum + ","
                        + "\"listCnt\":" + LIST_COUNT + ","
                        + "\"cont10\":\"PC\""
                        + "}";

                HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("[굽네] {} 목록 조회 실패(pageNum={}): status={}", eventGb, pageNum, response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode brdList = root.path("result").path("brdList");

                if (!brdList.isArray() || brdList.isEmpty()) {
                    break;
                }

                totalCnt = brdList.get(0).path("total_cnt").asInt(result.size() + brdList.size());

                for (JsonNode node : brdList) {
                    try {
                        result.add(toCreateEvent(node, isOngoing, brandId));
                    } catch (Exception e) {
                        log.error("[굽네] 개별 항목 파싱 오류: {}", e.getMessage());
                    }
                }

                pageNum++;
            } catch (Exception e) {
                log.error("[굽네] {} 목록 조회 오류(pageNum={}): {}", eventGb, pageNum, e.getMessage());
                break;
            }
        }

        log.info("[굽네] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private CreateEvent toCreateEvent(JsonNode node, boolean isOngoing, Long brandId) {
        String seq = node.get("seq").asText();
        String title = node.get("title").asText().trim();
        String linkUrl = DETAIL_URL_PREFIX + seq;
        String imgUrl = IMG_BASE_URL + node.get("file01").asText();

        LocalDate start = LocalDate.parse(node.get("s_time").asText(), DATE_FORMAT);
        LocalDate end = LocalDate.parse(node.get("e_time").asText(), DATE_FORMAT);
        LocalDateTime startDate = start.atStartOfDay();
        LocalDateTime endDate = end.atTime(23, 59, 59);

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