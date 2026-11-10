package com.backend.seo.service;

import com.backend.event.dto.EventCode;
import com.backend.event.entity.Event;
import com.backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeoService {
    private static final List<String> STATIC_PATHS = List.of("/", "/terms", "/privacy");
    private static final String SITE_NAME = "이츠어딜!";
    private static final String DEFAULT_DESCRIPTION = "치킨, 피자, 햄버거 프랜차이즈의 할인·1+1·쿠폰 이벤트를 한곳에서 확인하세요.";
    private static final String NOT_FOUND_TITLE = "이벤트를 찾을 수 없음 | " + SITE_NAME;
    private static final int MAX_DESCRIPTION_LENGTH = 150;
    private static final int ALWAYS_ON_YEAR = 2099;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final Pattern TITLE_TAG = Pattern.compile("<title>[^<]*</title>");
    private static final Pattern OG_IMAGE_SIZE = Pattern.compile("<meta property=\"og:image:(?:width|height)\"[^>]*>\\s*");
    private static final String ROOT_DIV = "<div id=\"root\"></div>";
    private static final String HEAD_END = "</head>";

    public record EventPage(boolean found, String html) {
    }

    private record PageMeta(String title, String description, String url, String image) {
    }

    private final EventRepository eventRepository;
    private final SpaIndexClient spaIndexClient;

    @Value("${app.site-url:https://www.eatsadeal.kr}")
    private String siteUrl;

    @Transactional(readOnly = true)
    public String buildSitemap() {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        STATIC_PATHS.forEach(path -> appendUrl(xml, path));
        eventRepository.findVisibleIds(LocalDateTime.now())
                .forEach(id -> appendUrl(xml, "/events/" + id));
        return xml.append("</urlset>\n").toString();
    }

    private void appendUrl(StringBuilder xml, String path) {
        xml.append("  <url><loc>").append(siteUrl).append(path).append("</loc></url>\n");
    }

    @Transactional(readOnly = true)
    public Optional<EventPage> buildEventPage(Long eventId) {
        Optional<String> template = spaIndexClient.fetch();
        if (template.isEmpty()) {
            return Optional.empty();
        }
        String url = siteUrl + "/events/" + eventId;
        Optional<Event> found = eventRepository.findVisibleById(eventId);
        if (found.isEmpty()) {
            PageMeta meta = new PageMeta(NOT_FOUND_TITLE, DEFAULT_DESCRIPTION, url, defaultImage());
            return Optional.of(new EventPage(false, render(template.get(), meta, "", true)));
        }
        Event event = found.get();
        String description = StringUtils.hasText(event.getDescription())
                ? event.getDescription()
                : fallbackDescription(event);
        PageMeta meta = new PageMeta(
                event.getTitle() + " | " + SITE_NAME,
                truncate(description),
                url,
                eventImage(event));
        return Optional.of(new EventPage(true, render(template.get(), meta, summary(event), false)));
    }

    private String render(String template, PageMeta meta, String body, boolean noindex) {
        String title = escape(meta.title());
        String description = escape(meta.description());
        String url = escape(meta.url());
        String image = escape(meta.image());

        String html = TITLE_TAG.matcher(template).replaceFirst(Matcher.quoteReplacement("<title>" + title + "</title>"));
        html = replaceAttr(html, "name=\"description\" content=\"", description);
        html = replaceAttr(html, "rel=\"canonical\" href=\"", url);
        html = replaceAttr(html, "property=\"og:url\" content=\"", url);
        html = replaceAttr(html, "property=\"og:title\" content=\"", title);
        html = replaceAttr(html, "property=\"og:description\" content=\"", description);
        html = replaceAttr(html, "property=\"og:image\" content=\"", image);
        html = replaceAttr(html, "name=\"twitter:title\" content=\"", title);
        html = replaceAttr(html, "name=\"twitter:description\" content=\"", description);
        html = replaceAttr(html, "name=\"twitter:image\" content=\"", image);
        if (!meta.image().equals(defaultImage())) {
            html = OG_IMAGE_SIZE.matcher(html).replaceAll("");
        }
        if (noindex) {
            html = html.replaceFirst(HEAD_END, Matcher.quoteReplacement("<meta name=\"robots\" content=\"noindex\" />\n" + HEAD_END));
        }
        return html.replace(ROOT_DIV, "<div id=\"root\">" + body + "</div>");
    }

    private static String replaceAttr(String html, String prefix, String value) {
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "[^\"]*\"");
        return pattern.matcher(html).replaceFirst(Matcher.quoteReplacement(prefix + value + "\""));
    }

    private String summary(Event event) {
        StringBuilder body = new StringBuilder("<main>")
                .append("<h1>").append(escape(event.getTitle())).append("</h1>");
        String labels = eventCodeLabels(event);
        String brandLine = labels.isEmpty() ? event.getBrand().getName() : event.getBrand().getName() + " · " + labels;
        body.append("<p>").append(escape(brandLine)).append("</p>")
                .append("<p>기간: ").append(escape(period(event))).append("</p>");
        if (StringUtils.hasText(event.getDescription())) {
            body.append("<p>").append(escape(event.getDescription())).append("</p>");
        }
        if (event.getUrl() != null && event.getUrl().startsWith("http")) {
            body.append("<p><a href=\"").append(escape(event.getUrl()))
                    .append("\" rel=\"nofollow noopener\">공식 이벤트 페이지</a></p>");
        }
        body.append("<p><a href=\"/\">").append(escape(SITE_NAME)).append(" 전체 이벤트 보기</a></p>")
                .append("</main>");
        return body.toString();
    }

    private static String fallbackDescription(Event event) {
        String labels = eventCodeLabels(event);
        String prefix = labels.isEmpty()
                ? event.getBrand().getName()
                : event.getBrand().getName() + " " + labels;
        return prefix + " 이벤트: " + event.getTitle() + " (" + period(event) + ")";
    }

    private static String eventCodeLabels(Event event) {
        return event.getEventCodes().stream()
                .sorted(Comparator.comparingInt(EventCode::ordinal))
                .map(EventCode::getTitle)
                .collect(Collectors.joining(", "));
    }

    private static String period(Event event) {
        String start = event.getStartDate().format(DATE_FORMAT);
        LocalDateTime end = event.getEndDate();
        String endText = end == null || end.getYear() >= ALWAYS_ON_YEAR ? "상시" : end.format(DATE_FORMAT);
        return start + " ~ " + endText;
    }

    private String eventImage(Event event) {
        return event.getImg() != null && event.getImg().startsWith("https://")
                ? event.getImg()
                : defaultImage();
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private String defaultImage() {
        return siteUrl + "/og-image.png";
    }

    private static String truncate(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= MAX_DESCRIPTION_LENGTH ? oneLine : oneLine.substring(0, MAX_DESCRIPTION_LENGTH - 1) + "…";
    }
}
