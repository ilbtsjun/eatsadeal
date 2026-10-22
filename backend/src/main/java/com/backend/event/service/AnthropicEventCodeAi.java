package com.backend.event.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.UrlImageSource;
import com.backend.event.dto.EventCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class AnthropicEventCodeAi {
    private static final long MAX_OUTPUT_TOKENS = 60L;

    private static final String SYSTEM_PROMPT = """
            You classify Korean food-franchise promotion events into promotion types.
            Allowed codes:
            %s
            Reply with the matching codes separated by commas, and nothing else.
            If the event is not a promotion of any listed type, reply NONE.
            """.formatted(Stream.of(EventCode.values())
            .map(code -> code.name() + ": " + code.getDescription())
            .collect(Collectors.joining("\n")));

    private final boolean useImage;
    private final String model;
    private final int dailyLimit;
    private final AnthropicClient client;

    private LocalDate usageDate = LocalDate.now();
    private int usedToday;

    public AnthropicEventCodeAi(
            @Value("${app.ai.enabled:true}") boolean enabled,
            @Value("${app.ai.use-image:true}") boolean useImage,
            @Value("${app.ai.model:claude-haiku-5-5}") String model,
            @Value("${app.ai.daily-limit:200}") int dailyLimit,
            @Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        this.useImage = useImage;
        this.model = model;
        this.dailyLimit = dailyLimit;
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
        boolean withImage = StringUtils.hasText(imageUrl);
        if (client == null || (withImage && !useImage) || !tryAcquire()) {
            return EnumSet.noneOf(EventCode.class);
        }
        try {
            Message response = client.messages().create(buildParams(title, description, withImage ? imageUrl : null));
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining(" "));
            log.info("[AI] 분류 title={} image={} -> {} (in={}, out={})", title, withImage, text.trim(),
                    response.usage().inputTokens(), response.usage().outputTokens());
            return parseCodes(text);
        } catch (Exception e) {
            log.warn("[AI] 분류 실패 title={} image={}: {}", title, withImage, e.getMessage());
            return EnumSet.noneOf(EventCode.class);
        }
    }

    public synchronized int usedToday() {
        resetIfNewDay();
        return usedToday;
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

    private MessageCreateParams buildParams(String title, String description, String imageUrl) {
        String prompt = "Title: " + title + (StringUtils.hasText(description) ? "\nDescription: " + description : "");
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_OUTPUT_TOKENS)
                .system(SYSTEM_PROMPT);
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

    private synchronized boolean tryAcquire() {
        resetIfNewDay();
        if (usedToday >= dailyLimit) {
            log.warn("[AI] 오늘 호출 상한({})에 도달해 AI 분류를 건너뜁니다.", dailyLimit);
            return false;
        }
        usedToday++;
        return true;
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(usageDate)) {
            usageDate = today;
            usedToday = 0;
        }
    }
}
