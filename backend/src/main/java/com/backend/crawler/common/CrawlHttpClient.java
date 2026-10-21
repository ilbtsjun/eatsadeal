package com.backend.crawler.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CrawlHttpClient {

    public static final String USER_AGENT =
            "EatsADealPortfolioCrawler/1.0 (+mailto:ilbtsjun@gmail.com; https://github.com/ilbtsjun/eatsadeal)";

    private static final Map<String, Long> HOST_INTERVAL_MS = Map.of("pizzamaru.co.kr", 2000L);

    private final HttpClient client;
    private final Duration requestTimeout;
    private final long defaultIntervalMs;
    private final int maxRetries;
    private final long backoffMs;
    private final Map<String, Long> lastRequestAt = new ConcurrentHashMap<>();
    private final Map<String, Object> hostLocks = new ConcurrentHashMap<>();

    public CrawlHttpClient(
            @Value("${app.crawler.http.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${app.crawler.http.request-timeout-ms:15000}") long requestTimeoutMs,
            @Value("${app.crawler.http.min-interval-ms:500}") long defaultIntervalMs,
            @Value("${app.crawler.http.max-retries:2}") int maxRetries,
            @Value("${app.crawler.http.backoff-ms:1000}") long backoffMs) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager())
                .build();
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.defaultIntervalMs = defaultIntervalMs;
        this.maxRetries = maxRetries;
        this.backoffMs = backoffMs;
    }

    public <T> HttpResponse<T> send(HttpRequest original, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        HttpRequest request = prepare(original);
        String host = request.uri().getHost() == null ? "" : request.uri().getHost();
        int attempt = 0;

        while (true) {
            waitForTurn(host);
            try {
                HttpResponse<T> response = client.send(request, handler);
                if (!isRetryable(response.statusCode()) || attempt >= maxRetries) {
                    return response;
                }
                log.warn("[HTTP RETRY] host={} status={} attempt={}/{}", host, response.statusCode(), attempt + 1, maxRetries);
            } catch (IOException e) {
                if (attempt >= maxRetries) {
                    throw e;
                }
                log.warn("[HTTP RETRY] host={} error={} attempt={}/{}", host, e.getClass().getSimpleName(), attempt + 1, maxRetries);
            }
            attempt++;
            Thread.sleep(backoffMs * (1L << (attempt - 1)));
        }
    }

    // User-Agent는 공통 값으로 교체하고, 타임아웃이 없는 요청에만 기본 타임아웃을 붙인다
    private HttpRequest prepare(HttpRequest original) {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(original, (name, value) -> !name.equalsIgnoreCase("User-Agent"))
                .header("User-Agent", USER_AGENT);
        if (original.timeout().isEmpty()) {
            builder.timeout(requestTimeout);
        }
        return builder.build();
    }

    private boolean isRetryable(int status) {
        return status == 429 || status >= 500;
    }

    private void waitForTurn(String host) throws InterruptedException {
        synchronized (hostLocks.computeIfAbsent(host, h -> new Object())) {
            long interval = intervalFor(host);
            Long last = lastRequestAt.get(host);
            if (last != null) {
                long wait = last + interval - System.currentTimeMillis();
                if (wait > 0) {
                    Thread.sleep(wait);
                }
            }
            lastRequestAt.put(host, System.currentTimeMillis());
        }
    }

    private long intervalFor(String host) {
        return HOST_INTERVAL_MS.entrySet().stream()
                .filter(e -> host.endsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultIntervalMs);
    }
}