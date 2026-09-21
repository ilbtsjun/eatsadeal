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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BHC implements Crawler {
    private final BrandRepository brandRepository;

    private static final String API_URL = "https://www.bhc.co.kr/api/v1/web/events/list";
    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final String ACTIVE_DETAIL_URL_PREFIX = "https://www.bhc.co.kr/event/currentEvent/";
    private static final String ENDED_LIST_URL = "https://www.bhc.co.kr/event/endedEvent/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BHC";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .GET()
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[BHC] 이벤트 목록 조회 실패: status={}", response.statusCode());
                return eventList;
            }

            JsonNode root = objectMapper.readTree(response.body());
            // 응답 형식: {"status":"success","code":200,"body":[ {...}, ... ]}
            for (JsonNode node : root.path("body")) {
                try {
                    eventList.add(toCreateEvent(node));
                } catch (Exception e) {
                    log.error("[BHC] 개별 항목 파싱 오류: {}", e.getMessage());
                }
            }

            log.info("[BHC] 크롤링된 이벤트 개수: {}", eventList.size());
        } catch (Exception e) {
            log.error("[BHC] 크롤링 실패: {}", e.getMessage());
        }

        return eventList;
    }

    private CreateEvent toCreateEvent(JsonNode node) {
        String title = node.get("eventNm").asText().trim();
        String imgUrl = node.get("thumbnail").asText();
        boolean isActive = "ACTIVE".equalsIgnoreCase(node.get("eventStatus").asText());
        String linkUrl = isActive
                ? ACTIVE_DETAIL_URL_PREFIX + node.get("eventIdx").asText()
                : ENDED_LIST_URL + node.get("eventIdx").asText();

        LocalDateTime startDate = LocalDateTime.parse(node.get("startDate").asText());
        LocalDateTime endDate = LocalDateTime.parse(node.get("endDate").asText());

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