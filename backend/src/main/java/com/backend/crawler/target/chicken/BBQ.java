package com.backend.crawler.target.chicken;

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

@Slf4j
@Component
@AllArgsConstructor
public class BBQ implements Crawler {
    private final BrandRepository brandRepository;

    private static final String API_URL = "https://bbq.co.kr/api/delivery/content/event";
    private static final int PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final String SEARCH_TYPE_ONGOING = "OPEN";
    private static final String SEARCH_TYPE_CLOSED = "CLOSED";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BBQ";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        eventList.addAll(fetchAll(SEARCH_TYPE_ONGOING, true));
        eventList.addAll(fetchAll(SEARCH_TYPE_CLOSED, false));
        return eventList;
    }

    private List<CreateEvent> fetchAll(String searchType, boolean isOngoing) {
        List<CreateEvent> result = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            try {
                String url = API_URL + "?searchType=" + searchType + "&page=" + page + "&size=" + PAGE_SIZE;
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("[BBQ] {} 목록 조회 실패(page={}): status={}", searchType, page, response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                for (JsonNode node : root.get("content")) {
                    try {
                        result.add(toCreateEvent(node, isOngoing));
                    } catch (Exception e) {
                        log.error("[BBQ] 개별 항목 파싱 오류: {}", e.getMessage());
                    }
                }

                last = root.path("last").asBoolean(true);
                page++;
            } catch (Exception e) {
                log.error("[BBQ] {} 목록 조회 오류(page={}): {}", searchType, page, e.getMessage());
                break;
            }
        }

        log.info("[BBQ] searchType={} 이벤트 개수: {}", searchType, result.size());
        return result;
    }

    private CreateEvent toCreateEvent(JsonNode node, boolean isOngoing) {
        String title = node.get("title").asText();
        String linkUrl = "https://bbq.co.kr/events/" + node.get("id").asText();
        String imgUrl = node.get("thumbnailImageUrl").asText();


        LocalDateTime startDate = LocalDateTime.parse(node.get("startsAt").asText(), DATE_TIME_FORMAT);
        LocalDateTime endDate = LocalDateTime.parse(node.get("endsAt").asText(), DATE_TIME_FORMAT);

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imgUrl,
                startDate,
                endDate,
                brandRepository.findByName(getName()).getId(),
                isOngoing,
                null);
    }
}