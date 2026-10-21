package com.backend.crawler.target.pizza;

import com.backend.brand.entity.Brand;
import com.backend.crawler.common.CrawlErrors;
import com.backend.crawler.common.CrawlHttpClient;
import com.backend.crawler.common.CrawlResult;
import com.backend.crawler.common.CrawlerUtil;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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
    private static final String BASE_URL = "https://pji.co.kr";
    private static final String ONGOING_URL = BASE_URL + "/event/ongoing";
    private static final String PAST_URL = BASE_URL + "/event/past-event";

    private static final int MAX_PAGES = 50; // 무한 루프 방지 안전장치
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<a\\s+[^>]*href=\"(/event/(?:ongoing|past-event)/\\d+)\"[^>]*>(.*?)</a>", Pattern.DOTALL);

    private static final Pattern ONGOING_TITLE_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*line-clamp-2[^\"]*\"[^>]*>([^<]+)</span>");
    private static final Pattern DATE_CONTAINER_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*text-bg-neutral-default[^\"]*\"[^>]*>(.*?)</span>", Pattern.DOTALL);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*src=\"([^\"]+)\"");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final Pattern PAST_TITLE_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*font-semibold[^\"]*\"[^>]*>([^<]+)</span>");
    private static final Pattern FLEXIBLE_DATE_PATTERN = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");

    private final CrawlHttpClient http;

    @Override
    public String getName() {
        return "Papajohns";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();
        eventList.addAll(fetchAll(ONGOING_URL, true, brand.getId(), errors));
        eventList.addAll(fetchAll(PAST_URL, false, brand.getId(), errors));
        return CrawlResult.of(eventList, errors);
    }

    private List<CreateEvent> fetchAll(String listUrl, boolean isOngoing, Long brandId, CrawlErrors errors) {
        List<CreateEvent> result = new ArrayList<>();
        Set<String> seenHrefs = new HashSet<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = page == 1 ? listUrl : listUrl + "?page=" + page;

            String html;
            try {
                html = http.getString(url);
            } catch (Exception e) {
                log.error("[파파존스] 목록 조회 오류(url={}): {}", url, e.toString());
                errors.add("목록 조회 url=" + url, e);
                break;
            }

            Matcher cardMatcher = CARD_PATTERN.matcher(html);
            boolean foundNew = false;

            while (cardMatcher.find()) {
                String href = cardMatcher.group(1);
                if (!seenHrefs.add(href)) {
                    continue;
                }
                foundNew = true;
                try {
                    if(isOngoing) {
                        result.add(toCreateEventOngoing(href, cardMatcher.group(2), isOngoing, brandId));
                    }
                    else{
                        result.add(toCreateEventPast(href, cardMatcher.group(2), isOngoing, brandId));
                    }
                } catch (Exception e) {
                    log.error("[파파존스] 개별 항목 파싱 오류(href={}): {}", href, e.toString());
                    errors.add("항목 파싱 href=" + href, e);
                }
            }

            if (!foundNew) {
                break;
            }
        }

        log.info("[파파존스] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private CreateEvent toCreateEventOngoing(String href, String cardHtml, boolean isActive, Long brandId) {
        String linkUrl = BASE_URL + href;

        // 이미지 경로 추출
        Matcher imgMatcher = IMG_SRC_PATTERN.matcher(cardHtml);
        String imgUrl = imgMatcher.find() ? imgMatcher.group(1) : "";

        // 제목 추출 (HTML 엔티티 해제)
        Matcher titleMatcher = ONGOING_TITLE_PATTERN.matcher(cardHtml);
        if (!titleMatcher.find()) {
            throw new IllegalArgumentException("제목 파싱 실패");
        }
        String title = HtmlUtils.htmlUnescape(titleMatcher.group(1).trim());

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
            startDate = CrawlerUtil.startOfDay(LocalDate.parse(dateMatcher.group(1), DATE_FORMAT));
        }
        if (dateMatcher.find()) {
            endDate = CrawlerUtil.endOfDay(LocalDate.parse(dateMatcher.group(1), DATE_FORMAT));
        } else {
            // 종료일이 없는 상시 이벤트
            endDate = CrawlerUtil.openEndedEnd();
        }

        if (startDate == null) {
            throw new IllegalArgumentException("시작일 파싱 실패");
        }

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId, isActive);
    }

    private CreateEvent toCreateEventPast(String href, String listHtml, boolean isActive, Long brandId) {
        String linkUrl = BASE_URL + href;

        // 이미지 경로 (지난 이벤트 리스트는 이미지 없음)
        String imgUrl = "";

        // 제목 추출 (font-semibold 기준, 없으면 태그를 지운 전체 텍스트)
        Matcher titleMatcher = PAST_TITLE_PATTERN.matcher(listHtml);
        String title = titleMatcher.find()
                ? HtmlUtils.htmlUnescape(titleMatcher.group(1).trim())
                : CrawlerUtil.cleanText(listHtml);

        // 2. 유연한 날짜 파싱 (2026.09.01, 2026-9-1 등 모두 대응)
        Matcher dateMatcher = FLEXIBLE_DATE_PATTERN.matcher(listHtml);
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (dateMatcher.find()) {
            startDate = CrawlerUtil.startOfDay(toDate(dateMatcher));
        }

        if (dateMatcher.find()) {
            endDate = CrawlerUtil.endOfDay(toDate(dateMatcher));
        } else {
            // 종료일이 없는 경우
            endDate = CrawlerUtil.openEndedEnd();
        }

        // 날짜가 아예 없으면 오늘 날짜를 넣지 않고 null로 넘겨 기존 날짜를 유지한다(매일 "변경"으로 잡히지 않게)
        if (startDate == null) {
            return CrawlerUtil.event(title, linkUrl, imgUrl, null, null, brandId, isActive);
        }

        return CrawlerUtil.event(title, linkUrl, imgUrl, startDate, endDate, brandId, isActive);
    }

    private LocalDate toDate(Matcher matcher) {
        return LocalDate.of(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }
}