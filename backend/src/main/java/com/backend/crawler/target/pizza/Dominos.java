package com.backend.crawler.target.pizza;

import com.backend.brand.entity.Brand;
import com.backend.crawler.common.CrawlErrors;
import com.backend.crawler.common.CrawlHttpClient;
import com.backend.crawler.common.CrawlResult;
import com.backend.crawler.common.Crawler;
import com.backend.event.dto.CreateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
public class Dominos implements Crawler {
    private static final String BASE_URL = "https://web.dominos.co.kr";
    private static final String LIST_URL = BASE_URL + "/event/list?gubun=E0200";
    private static final String ENDED_LIST_URL = BASE_URL + "/event/endlist";
    private static final String AJAX_URL = BASE_URL + "/event/eventListAjax";
    private static final String IMG_BASE_URL = "https://cdn.dominos.co.kr/admin/upload/event/";

    private static final Charset SITE_CHARSET = Charset.forName("EUC-KR");

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<a\\s+href=\"([^\"]+)\"[^>]*>\\s*<img([^>]*)>", Pattern.DOTALL);
    private static final Pattern SRC_PATTERN = Pattern.compile("\\bsrc=\"([^\"]+)\"");
    private static final Pattern DATA_SRC_PATTERN = Pattern.compile("data-src=\"([^\"]+)\"");
    private static final Pattern ALT_PATTERN = Pattern.compile("alt=\"([^\"]*)\"");
    private static final Pattern JS_CALL_PATTERN = Pattern.compile("(\\w+)\\(([^)]*)\\)");

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<h2 class=\"title-type\">(.*?)</h2>", Pattern.DOTALL);
    private static final Pattern DATE_BLOCK_PATTERN =
            Pattern.compile("<p class=\"date\">(.*?)</p>", Pattern.DOTALL);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final CrawlHttpClient http;

    @Override
    public String getName() {
        return "Dominos";
    }

    @Override
    public CrawlResult crawl(Brand brand) {
        List<CreateEvent> eventList = new ArrayList<>();
        CrawlErrors errors = new CrawlErrors();
        eventList.addAll(fetchAll(LIST_URL, "E0200", true, brand.getId(), errors));
        eventList.addAll(fetchAll(ENDED_LIST_URL, "E0200", false, brand.getId(), errors));
        return CrawlResult.of(dedupeByUrl(eventList), errors);
    }

    private List<CreateEvent> dedupeByUrl(List<CreateEvent> events) {
        Map<String, CreateEvent> byUrl = new LinkedHashMap<>();
        for (CreateEvent event : events) {
            byUrl.putIfAbsent(event.url(), event);
        }
        return new ArrayList<>(byUrl.values());
    }

    private List<CreateEvent> fetchAll(String listUrl, String gubun, boolean isOngoing, Long brandId, CrawlErrors errors) {
        Map<String, String[]> items = new LinkedHashMap<>();

        try {
            String html = fetchAsString(listUrl);
            parseListHtml(html, items);
        } catch (Exception e) {
            log.error("[도미노] 목록 조회 오류(url={}): {}", listUrl, e.toString());
            errors.add("목록 조회 url=" + listUrl, e);
        }

        int pageNo = items.isEmpty() ? 1 : 2;
        while (true) {
            try {
                String body = "gubun=" + gubun + "&pageNo=" + pageNo;
                HttpRequest request = HttpRequest.newBuilder(URI.create(AJAX_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    errors.add("AJAX 목록 조회 pageNo=" + pageNo + " status=" + response.statusCode());
                    break;
                }
                if (response.body().isBlank()) {
                    break;
                }
                int before = items.size();
                parseAjaxJson(response.body(), gubun, items);
                if (items.size() == before) {
                    break;
                }
                pageNo++;
                if (pageNo > 50) {
                    break;
                }
            } catch (Exception e) {
                log.error("[도미노] AJAX 목록 조회 오류(pageNo={}): {}", pageNo, e.toString());
                errors.add("AJAX 목록 조회 pageNo=" + pageNo, e);
                break;
            }
        }

        List<CreateEvent> result = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : items.entrySet()) {
            try {
                CreateEvent event = fetchDetail(entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], isOngoing, brandId);
                if (event != null) {
                    result.add(event);
                }
            } catch (Exception e) {
                log.error("[도미노] 상세 조회 오류(href={}): {}", entry.getValue()[0], e.toString());
                errors.add("상세 조회 href=" + entry.getValue()[0], e);
            }
        }

        log.info("[도미노] {} 이벤트 개수: {}", isOngoing ? "진행중" : "종료된", result.size());
        return result;
    }

    private void parseListHtml(String html, Map<String, String[]> items) {
        Matcher itemMatcher = ITEM_PATTERN.matcher(html);
        while (itemMatcher.find()) {
            String rawHref = itemMatcher.group(1).trim();
            String imgAttrs = itemMatcher.group(2);

            String title = firstGroup(ALT_PATTERN, imgAttrs, "");
            String imgSrc = firstGroup(DATA_SRC_PATTERN, imgAttrs, null);
            if (imgSrc == null) {
                imgSrc = firstGroup(SRC_PATTERN, imgAttrs, "");
            }

            String detailUrl = resolveDetailUrl(rawHref);
            if (detailUrl == null) {
                continue;
            }

            items.putIfAbsent(detailUrl, new String[] { detailUrl, imgSrc, title });
        }
    }

    private String resolveDetailUrl(String rawHref) {
        if (!rawHref.startsWith("javascript:")) {
            return rawHref.startsWith("http") ? rawHref : BASE_URL + rawHref;
        }

        Matcher callMatcher = JS_CALL_PATTERN.matcher(rawHref);
        if (!callMatcher.find()) {
            return null;
        }

        String funcName = callMatcher.group(1);
        String[] args = splitJsArgs(callMatcher.group(2));

        switch (funcName) {
            case "goEntryView":
                return BASE_URL + "/event/entry/" + args[1];
            case "goViewHtml":
                return BASE_URL + "/event/viewHtml?seq=" + args[1] + "&gubun=" + args[3];
            case "goView":
                return BASE_URL + "/event/view?seq=" + args[1] + "&gubun=" + args[2];
            case "goLinkView": {
                String linkUrl = args[2];
                return linkUrl.startsWith("http") ? linkUrl : BASE_URL + linkUrl;
            }
            default:
                return null;
        }
    }

    private String[] splitJsArgs(String rawArgs) {
        String[] parts = rawArgs.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim().replaceAll("^'|'$", "");
        }
        return result;
    }

    private void parseAjaxJson(String json, String gubun, Map<String, String[]> items) {
        Pattern entryPattern = Pattern.compile(
                "\\{[^{}]*\"seq\"\\s*:\\s*\"?(\\d+)\"?[^{}]*\\}", Pattern.DOTALL);
        Matcher entryMatcher = entryPattern.matcher(json);

        while (entryMatcher.find()) {
            String entryJson = entryMatcher.group();
            String seq = entryMatcher.group(1);
            if ("0".equals(seq)) {
                continue;
            }
            String type = extractJsonField(entryJson, "type");
            String linkUrl = extractJsonField(entryJson, "link_url");
            String target = extractJsonField(entryJson, "target");
            String fileNm = extractJsonField(entryJson, "file_nm");
            String title = extractJsonField(entryJson, "title");
            String itemGubun = extractJsonField(entryJson, "gubun");
            if (itemGubun == null) {
                itemGubun = gubun;
            }

            String detailUrl;
            if (type != null && !type.isBlank()) {
                detailUrl = BASE_URL + "/event/entry/" + seq;
            } else if (linkUrl != null && !linkUrl.isBlank()) {
                if (linkUrl.contains(".html")) {
                    detailUrl = BASE_URL + "/event/viewHtml?seq=" + seq + "&gubun=" + itemGubun;
                } else {
                    detailUrl = linkUrl.startsWith("http") ? linkUrl : BASE_URL + linkUrl;
                }
            } else {
                detailUrl = BASE_URL + "/event/view?seq=" + seq + "&gubun=" + itemGubun;
            }

            String imgUrl = fileNm != null ? IMG_BASE_URL + fileNm : "";
            items.putIfAbsent(detailUrl, new String[] { detailUrl, imgUrl, title == null ? "" : title });
        }
    }

    private String extractJsonField(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private String firstGroup(Pattern pattern, String input, String defaultValue) {
        Matcher m = pattern.matcher(input);
        return m.find() ? m.group(1) : defaultValue;
    }

    private CreateEvent fetchDetail(String detailUrl, String imgUrl, String fallbackTitle, boolean isOngoing, Long brandId)
            throws Exception {
        String html = fetchAsString(detailUrl);

        Matcher titleMatcher = TITLE_PATTERN.matcher(html);
        String title = titleMatcher.find() ? titleMatcher.group(1).trim() : fallbackTitle;
        if (title.isBlank()) {
            throw new IllegalArgumentException("제목 파싱 실패(url=" + detailUrl + ")");
        }

        LocalDateTime startDate;
        LocalDateTime endDate;
        boolean isActive;

        Matcher dateBlockMatcher = DATE_BLOCK_PATTERN.matcher(html);
        String startDateStr = null;
        String endDateStr = null;
        if (dateBlockMatcher.find()) {
            Matcher dateMatcher = DATE_PATTERN.matcher(dateBlockMatcher.group(1));
            startDateStr = dateMatcher.find() ? dateMatcher.group() : null;
            endDateStr = dateMatcher.find() ? dateMatcher.group() : null;
        }

        if (startDateStr != null && endDateStr != null) {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();
            endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);
            isActive = isOngoing;
        } else {
            startDate = null;
            endDate = null;
            isActive = true;
        }

        return new CreateEvent(
                title,
                null,
                detailUrl,
                imgUrl,
                startDate,
                endDate,
                brandId,
                isActive,
                null);
    }

    private String fetchAsString(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return new String(response.body(), SITE_CHARSET);
    }
}