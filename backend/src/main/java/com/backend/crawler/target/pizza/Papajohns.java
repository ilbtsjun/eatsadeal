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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class Papajohns implements Crawler {
    private final BrandRepository brandRepository;

    private static final String BASE_URL = "https://pji.co.kr";
    private static final String ONGOING_URL = BASE_URL + "/event/ongoing";
    private static final String PAST_URL = BASE_URL + "/event/past-event";

    private static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final int MAX_PAGES = 50; // 무한 루프 방지 안전장치
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(2099, 12, 31);

    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<a\\s+[^>]*href=\"(/event/(?:ongoing|past-event)/\\d+)\"[^>]*>(.*?)</a>", Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "Papajohns";
    }

    @Override
    public List<CreateEvent> crawl() {
        List<CreateEvent> eventList = new ArrayList<>();
        eventList.addAll(fetchAll(ONGOING_URL, true));
        eventList.addAll(fetchAll(PAST_URL, false));
        return eventList;
    }

    private List<CreateEvent> fetchAll(String listUrl, boolean isOngoing) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = page == 1 ? listUrl : listUrl + "?page=" + page;

            String html;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("User-Agent", USER_AGENT)
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("[파파존스] 목록 조회 실패(url={}): status={}", url, response.statusCode());
                    break;
                }
                html = response.body();
            } catch (Exception e) {
                log.error("[파파존스] 목록 조회 오류(url={}): {}", url, e.getMessage());
                break;
            }

            Matcher cardMatcher = CARD_PATTERN.matcher(html);
            boolean foundNew = false;

            while (cardMatcher.find()) {
                String href = cardMatcher.group(1);
                log.warn(href);
                if (!seenHrefs.add(href)) {
                    continue;
                }
                foundNew = true;
                try {
                    if(isOngoing) {
                        result.add(toCreateEventOngoing(href, cardMatcher.group(2), isOngoing));
                    }
                    else{
                        result.add(toCreateEventPast(href, cardMatcher.group(2), isOngoing));
                    }
                } catch (Exception e) {
                    log.error("[파파존스] 개별 항목 파싱 오류(href={}): {}", href, e.getMessage());
                }
            }

            if (!foundNew) {
                break;
            }
        }

        log.info("[파파존스] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private CreateEvent toCreateEventOngoing(String href, String cardHtml, boolean isActive) {
        String linkUrl = BASE_URL + href;

       Pattern TITLE_PATTERN = Pattern.compile(
                "<span[^>]*class=\"[^\"]*line-clamp-2[^\"]*\"[^>]*>([^<]+)</span>");
        Pattern DATE_CONTAINER_PATTERN = Pattern.compile(
                "<span[^>]*class=\"[^\"]*text-bg-neutral-default[^\"]*\"[^>]*>(.*?)</span>", Pattern.DOTALL);
        Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*src=\"([^\"]+)\"");
        Pattern DATE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})");

        // 이미지 경로 추출
        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(cardHtml);
        String imgUrl = imgMatcher.find() ? imgMatcher.group(1) : "";

        // 제목 추출 (HTML 엔티티 해제)
        Matcher titleMatcher = TITLE_PATTERN.matcher(cardHtml);
        if (!titleMatcher.find()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }
        String title = unescapeHtml(titleMatcher.group(1).trim());

        // 날짜 영역 추출 및 주석(<!-- -->) 제거
        Matcher dateContainerMatcher = DATE_CONTAINER_PATTERN.matcher(cardHtml);
        if (!dateContainerMatcher.find()) {
            throw new IllegalArgumentException("날짜 영역 파싱 실패");
        }
        String cleanDateText = dateContainerMatcher.group(1).replaceAll("<!--.*?-->", "").trim();

        // 날짜 텍스트에서 YYYY.MM.DD 파싱
        Matcher dateMatcher = DATE_PATTERN.matcher(cleanDateText);
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (dateMatcher.find()) {
            startDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMAT).atStartOfDay();
        }
        if (dateMatcher.find()) {
            endDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMAT).atTime(23, 59, 59);
        } else {
            // 종료일이 없는 상시 이벤트
            endDate = OPEN_ENDED_DATE.atTime(23, 59, 59);
        }

        if (startDate == null) {
            throw new IllegalArgumentException("시작일 파싱 실패");
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
                startDate,
                endDate,
                brandId,
                isActive,
                null);
    }

    private static final Pattern FLEXIBLE_DATE_PATTERN = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");

    private CreateEvent toCreateEventPast(String href, String listHtml, boolean isActive) {
        String linkUrl = BASE_URL + href;

        // 1. 제목 추출 패턴 (font-semibold 기준 또는 단순 텍스트 추출)
        Pattern TITLE_PATTERN = Pattern.compile(
                "<span[^>]*class=\"[^\"]*font-semibold[^\"]*\"[^>]*>([^<]+)</span>");

        // 이미지 경로 (지난 이벤트 리스트는 이미지 없음)
        String imgUrl = "";

        // 제목 추출
        Matcher titleMatcher = TITLE_PATTERN.matcher(listHtml);
        String title;
        if (titleMatcher.find()) {
            title = unescapeHtml(titleMatcher.group(1).trim());
        } else {
            // 만약 클래스명이 다를 경우 태그를 제거하고 첫 줄을 제목으로 사용
            title = unescapeHtml(listHtml.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
        }

        // 2. 유연한 날짜 파싱 (2026.09.01, 2026-9-1 등 모두 대응)
        Matcher dateMatcher = FLEXIBLE_DATE_PATTERN.matcher(listHtml);
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (dateMatcher.find()) {
            int year = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));
            startDate = LocalDate.of(year, month, day).atStartOfDay();
        }

        if (dateMatcher.find()) {
            int year = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));
            endDate = LocalDate.of(year, month, day).atTime(23, 59, 59);
        } else {
            // 종료일이 없는 경우
            endDate = OPEN_ENDED_DATE.atTime(23, 59, 59);
        }

        // 날짜가 아예 없는 예외 케이스 방어 (크롤링 중단 방지)
        if (startDate == null) {
            startDate = LocalDate.now().atStartOfDay();
        }

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

    private String unescapeHtml(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}