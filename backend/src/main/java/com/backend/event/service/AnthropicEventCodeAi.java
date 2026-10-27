package com.backend.event.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.UrlImageSource;
import com.backend.common.redis.RedisRateLimiter;
import com.backend.event.dto.EventCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class AnthropicEventCodeAi {
    private static final long MAX_OUTPUT_TOKENS = 60L;
    private static final long MAX_OUTPUT_TOKENS_WITH_SUMMARY = 300L;
    public static final int MAX_SUMMARY_LENGTH = 255;
    private static final String USAGE_KEY_PREFIX = "ai:usage:";
    private static final Duration USAGE_KEY_TTL = Duration.ofDays(2);
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static final String SYSTEM_PROMPT = """
            You classify Korean food-franchise promotion events into promotion types.
            Allowed codes:
            %s
            If the event is not a promotion of any listed type, the codes are NONE.
            """.formatted(Stream.of(EventCode.values())
            .map(code -> code.name() + ": " + code.getDescription())
            .collect(Collectors.joining("\n")));

    private static final String CODES_ONLY_INSTRUCTION = """
            Reply with the matching codes separated by commas, and nothing else.
            """;

    private static final String SUMMARY_INSTRUCTION = """
            Also write a short Korean description of the event for customers.
            Rules for the description:
            - Use only what the banner image or title actually says. Never guess or invent details.
            - Include the benefit, target menu, period and conditions when they are shown.
            - One or two sentences, at most 120 characters.
            - If the banner text cannot be read, write NONE.
            Reply in exactly two lines and nothing else:
            CODES: <codes separated by commas, or NONE>
            SUMMARY: <description, or NONE>
            """;

    public record Analysis(Set<EventCode> codes, String summary) {
        static Analysis empty() {
            return new Analysis(EnumSet.noneOf(EventCode.class), null);
        }
    }

    private final boolean useImage;
    private final String model;
    private final int dailyLimit;
    private final AnthropicClient client;
    private final RedisRateLimiter redisRateLimiter;
    private final StringRedisTemplate redisTemplate;

    public AnthropicEventCodeAi(
            @Value("${app.ai.enabled:true}") boolean enabled,
            @Value("${app.ai.use-image:true}") boolean useImage,
            @Value("${app.ai.model:claude-haiku-5-5}") String model,
            @Value("${app.ai.daily-limit:200}") int dailyLimit,
            @Value("${ANTHROPIC_API_KEY:}") String apiKey,
            RedisRateLimiter redisRateLimiter,
            StringRedisTemplate redisTemplate) {
        this.useImage = useImage;
        this.model = model;
        this.dailyLimit = dailyLimit;
        this.redisRateLimiter = redisRateLimiter;
        this.redisTemplate = redisTemplate;
        if (enabled && StringUtils.hasText(apiKey)) {
            this.client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .maxRetries(1)
                    .build();
        } else {
            this.client = null;
            log.info("[AI] 이벤트 코드 AI 분류 꺼짐 (enabled={}, apiKey={})", enabled, StringUtils.hasText(apiKey) ? "있음" : "없음");
        }
    }

    public Set<EventCode> classify(String title, String description, String imageUrl) {
        return call(title, description, imageUrl, false).codes();
    }

    public Analysis analyzeBanner(String title, String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return Analysis.empty();
        }
        return call(title, null, imageUrl, true);
    }

    private Analysis call(String title, String description, String imageUrl, boolean withSummary) {
        boolean withImage = StringUtils.hasText(imageUrl);
        if (client == null || (withImage && !useImage) || !tryAcquire()) {
            return Analysis.empty();
        }
        try {
            Message response = client.messages().create(
                    buildParams(title, description, withImage ? imageUrl : null, withSummary));
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining("\n"));
            log.info("[AI] 분류 title={} image={} summary={} -> {} (in={}, out={})", title, withImage, withSummary,
                    text.trim().replace('\n', ' '), response.usage().inputTokens(), response.usage().outputTokens());
            return withSummary ? parseAnalysis(text) : new Analysis(parseCodes(text), null);
        } catch (Exception e) {
            log.warn("[AI] 분류 실패 title={} image={}: {}", title, withImage, e.getMessage());
            return Analysis.empty();
        }
    }

    public int usedToday() {
        try {
            String value = redisTemplate.opsForValue().get(usageKey());
            return value == null ? 0 : (int) Math.min(Long.parseLong(value), dailyLimit);
        } catch (Exception e) {
            log.warn("[AI] 사용량 조회 실패: {}", e.getMessage());
            return 0;
        }
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    public static Set<EventCode> parseCodes(String text) {
        Set<EventCode> codes = EnumSet.noneOf(EventCode.class);
        if (text == null) {
            return codes;
        }
        String upper = text.toUpperCase();
        for (EventCode code : EventCode.values()) {
            if (upper.contains(code.name())) {
                codes.add(code);
            }
        }
        return codes;
    }

    public static Analysis parseAnalysis(String text) {
        if (text == null) {
            return Analysis.empty();
        }
        String codesLine = null;
        String summary = null;
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "CODES:", 0, 6)) {
                codesLine = trimmed.substring(6);
            } else if (trimmed.regionMatches(true, 0, "SUMMARY:", 0, 8)) {
                summary = trimmed.substring(8).trim();
            }
        }
        Set<EventCode> codes = parseCodes(codesLine == null ? text : codesLine);
        if (!StringUtils.hasText(summary) || summary.equalsIgnoreCase("NONE")) {
            summary = null;
        } else if (summary.length() > MAX_SUMMARY_LENGTH) {
            summary = summary.substring(0, MAX_SUMMARY_LENGTH);
        }
        return new Analysis(codes, summary);
    }

    private MessageCreateParams buildParams(String title, String description, String imageUrl, boolean withSummary) {
        String prompt = "Title: " + title + (StringUtils.hasText(description) ? "\nDescription: " + description : "");
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(withSummary ? MAX_OUTPUT_TOKENS_WITH_SUMMARY : MAX_OUTPUT_TOKENS)
                .system(SYSTEM_PROMPT + (withSummary ? SUMMARY_INSTRUCTION : CODES_ONLY_INSTRUCTION));
        if (imageUrl == null) {
            return builder.addUserMessage(prompt).build();
        }
        return builder.addUserMessageOfBlockParams(List.of(
                ContentBlockParam.ofImage(ImageBlockParam.builder()
                        .source(UrlImageSource.builder().url(imageUrl).build())
                        .build()),
                ContentBlockParam.ofText(TextBlockParam.builder()
                        .text(prompt + "\nThe image is the event banner; use the text in it.")
                        .build()))).build();
    }

    private boolean tryAcquire() {
        try {
            if (!redisRateLimiter.tryAcquire(usageKey(), dailyLimit, USAGE_KEY_TTL)) {
                log.warn("[AI] 오늘 호출 상한({})에 도달해 AI 분류를 건너뜁니다.", dailyLimit);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("[AI] 사용량 기록 실패로 AI 분류를 건너뜁니다: {}", e.getMessage());
            return false;
        }
    }

    private String usageKey() {
        return USAGE_KEY_PREFIX + LocalDate.now(ZONE);
    }
}
