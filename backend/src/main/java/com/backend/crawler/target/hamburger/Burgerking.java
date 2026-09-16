package com.backend.crawler.target.hamburger;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
public class Burgerking implements Crawler {

    private final BrandRepository brandRepository;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://www.burgerking.co.kr";

    private static final String EVENT_API = BASE_URL + "/burgerking/BKR0608.json";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50;
    private static final int PAGE_COUNT = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(2099, 12, 31);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "Burgerking";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();

        Long brandId = brandRepository.findByName(getName()).getId();
        eventList.addAll(fetchAll("C", true, brandId));
        eventList.addAll(fetchAll("E", false, brandId));
        log.info("[버거킹] 총 이벤트 개수: {}", eventList.size());
        return eventList;
    }

    private List<CreateEvent> fetchAll(String status, boolean isActive, Long brandId) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenEventIds = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String json;
            try {
                json = requestEventList(status, page);
            } catch (Exception e) {
                log.error("[버거킹] 이벤트 목록 조회 오류(status={}, page={}): {}", status, e.getMessage());
                break;
            }

            try {
                JsonNode root = objectMapper.readTree(json);

                boolean success = root.path("header").path("result").asBoolean(false);

                if (!success) {
                    log.error("[버거킹] API 응답 실패(status={}, page={}, error={})", status, page,
                            root.path("header")
                                    .path("error_text")
                                    .asText()
                    );
                    break;
                }

                JsonNode eventList = root.path("body").path("eventList");

                if (!eventList.isArray() || eventList.isEmpty()) {
                    log.info("[버거킹] 이벤트 없음(status={}, page={})", status, page);
                    break;
                }

                int addedInThisPage = 0;

                for (JsonNode event : eventList) {

                    String eventId = event.path("eventId").asText();
                    if (eventId.isBlank()) {
                        continue;
                    }
                    if (!seenEventIds.add(eventId)) {
                        continue;
                    }

                    try {
                        result.add(toCreateEvent(event, isActive, brandId));
                        addedInThisPage++;

                    } catch (Exception e) {
                        log.error("[버거킹] 개별 이벤트 파싱 오류(eventId={}): {}", eventId, e.getMessage());
                    }
                }

                log.info("[버거킹] status={}, page={}, 수집 개수={}", status, page, addedInThisPage);

                if (addedInThisPage == 0) {
                    break;
                }

                if (eventList.size() < PAGE_COUNT) {
                    break;
                }

            } catch (Exception e) {
                log.error("[버거킹] JSON 파싱 오류(status={}, page={}): {}", status, page, e.getMessage());
                break;
            }
        }

        log.info("[버거킹] {} 이벤트 개수: {}", isActive ? "진행중" : "종료", result.size());

        return result;
    }

    private String requestEventList(String status, int page) throws Exception {
        String message = """
            {
              "header": {
                "result": true,
                "error_code": "",
                "error_text": "",
                "info_text": "",
                "message_version": "",
                "login_session_id": "",
                "trcode": "BKR0608",
                "cd_call_chnn": "01"
              },
              "body": {
                "cdTypeEvent": "00",
                "page": "%d",
                "pageCount": "20",
                "tpStatusEvent": "%s"
              }
            }
            """.formatted(page, status);

        String requestBody = "message=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(EVENT_API))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                )
                .header("Origin", BASE_URL)
                .header(
                        "Referer",
                        BASE_URL + (
                                "C".equals(status)
                                        ? "/event/ongoing"
                                        : "/event/end"
                        )
                )
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP status=" + response.statusCode() + ", body=" + response.body());
        }

        return response.body();
    }

    private CreateEvent toCreateEvent(JsonNode event, boolean isActive, Long brandId
    ) {
        String eventId = event.path("eventId").asText();
        String title = event.path("eventTitle").asText();
        String imageUrl = event.path("imageUrlWeb").asText();

        if (imageUrl.isBlank() || "null".equals(imageUrl)) {
            imageUrl = event.path("imageUrlApp").asText();
        }

        String startDateText = event.path("dtStart").asText();
        String endDateText = event.path("dtEnd").asText();

        if (title.isBlank()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }

        if (startDateText.isBlank()) {
            throw new IllegalArgumentException("시작일 파싱 실패");
        }

        LocalDate startDate = LocalDate.parse(startDateText, DATE_FORMAT);

        LocalDate endDate;
        if (endDateText.isBlank() || "99991231".equals(endDateText)) {
            endDate = OPEN_ENDED_DATE;
        } else {
            endDate = LocalDate.parse(endDateText, DATE_FORMAT);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        String linkUrl = BASE_URL + "/event/detail/" + eventId;

        return new CreateEvent(
                title,
                null,
                linkUrl,
                imageUrl,
                startDateTime,
                endDateTime,
                brandId,
                isActive,
                null
        );
    }
}