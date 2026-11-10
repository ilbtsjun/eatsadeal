package com.backend.seo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class SpaIndexClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @Value("${app.seo.index-url:http://frontend/index.html}")
    private String indexUrl;

    public Optional<String> fetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(indexUrl))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("index.html 응답 코드 {}: {}", response.statusCode(), indexUrl);
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("index.html 요청 중단: {}", indexUrl);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("index.html 요청 실패: {} ({})", indexUrl, e.toString());
            return Optional.empty();
        }
    }
}
