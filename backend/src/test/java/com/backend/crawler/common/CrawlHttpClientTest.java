package com.backend.crawler.common;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.backend.event.dto.CreateEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlHttpClientTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> seenUserAgent = new AtomicReference<>();
    private final AtomicReference<String> seenHeader = new AtomicReference<>();
    private final AtomicReference<String> seenContentType = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/flaky", ex -> reply(ex, hits.incrementAndGet() <= 2 ? 503 : 200));
        server.createContext("/down", ex -> { hits.incrementAndGet(); reply(ex, 503); });
        server.createContext("/missing", ex -> { hits.incrementAndGet(); reply(ex, 404); });
        server.createContext("/ua", ex -> {
            seenUserAgent.set(ex.getRequestHeaders().getFirst("User-Agent"));
            reply(ex, 200);
        });
        server.createContext("/text", ex -> {
            seenHeader.set(ex.getRequestHeaders().getFirst("X-Test"));
            replyBody(ex, 200, "hello 안녕".getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/euckr", ex -> replyBody(ex, 200, "안녕".getBytes(Charset.forName("EUC-KR"))));
        server.createContext("/echo", ex -> {
            seenContentType.set(ex.getRequestHeaders().getFirst("Content-Type"));
            seenHeader.set(ex.getRequestHeaders().getFirst("X-Test"));
            seenBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            replyBody(ex, 200, "ok".getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/slow", ex -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
            reply(ex, 200);
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    private static void replyBody(com.sun.net.httpserver.HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path))
                .GET().build();
    }

    private CrawlHttpClient client(long requestTimeoutMs, long intervalMs, int retries) {
        return new CrawlHttpClient(1000, requestTimeoutMs, intervalMs, retries, 10);
    }

    @Test
    void retriesOn5xxThenSucceeds() throws Exception {
        var res = client(2000, 0, 2).send(get("/flaky"), HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(hits.get()).isEqualTo(3);
    }

    @Test
    void returnsLast5xxAfterMaxRetries() throws Exception {
        var res = client(2000, 0, 2).send(get("/down"), HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(503);
        assertThat(hits.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryOn4xx() throws Exception {
        var res = client(2000, 0, 2).send(get("/missing"), HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(404);
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void replacesUserAgentWithSharedValue() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(get("/ua").uri())
                .header("User-Agent", "Mozilla/5.0").GET().build();
        client(2000, 0, 0).send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(seenUserAgent.get()).isEqualTo(CrawlHttpClient.USER_AGENT);
    }

    @Test
    void waitsMinIntervalBetweenRequestsToSameHost() throws Exception {
        CrawlHttpClient http = client(2000, 300, 0);
        long start = System.currentTimeMillis();
        http.send(get("/ua"), HttpResponse.BodyHandlers.ofString());
        http.send(get("/ua"), HttpResponse.BodyHandlers.ofString());
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(300);
    }

    @Test
    void timesOutWhenServerIsSlow() {
        assertThatThrownBy(() -> client(100, 0, 0).send(get("/slow"), HttpResponse.BodyHandlers.ofString()))
                .isInstanceOf(IOException.class);
    }
    @Test
    @DisplayName("getString: 200이면 본문을 돌려주고 요청 헤더를 붙인다")
    void getStringReturnsBodyAndSendsHeaders() throws Exception {
        String body = client(2000, 0, 0).getString(url("/text"), "X-Test", "abc");

        assertThat(body).isEqualTo("hello 안녕");
        assertThat(seenHeader.get()).isEqualTo("abc");
    }

    @Test
    @DisplayName("getString: 200이 아니면 상태 코드가 담긴 CrawlHttpException을 던진다")
    void getStringThrowsOnNon200() {
        assertThatThrownBy(() -> client(2000, 0, 0).getString(url("/missing")))
                .isInstanceOfSatisfying(CrawlHttpException.class, e -> {
                    assertThat(e.status()).isEqualTo(404);
                    assertThat(e.getMessage()).contains("404").contains("/missing");
                });
    }

    @Test
    @DisplayName("getString: 헤더 인자가 쌍이 아니면 IllegalArgumentException")
    void getStringRejectsOddHeaders() {
        assertThatThrownBy(() -> client(2000, 0, 0).getString(url("/text"), "X-Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getBytes: 원본 바이트를 돌려줘서 EUC-KR 같은 문자셋을 직접 풀 수 있다")
    void getBytesReturnsRawBody() throws Exception {
        byte[] body = client(2000, 0, 0).getBytes(url("/euckr"));

        assertThat(new String(body, Charset.forName("EUC-KR"))).isEqualTo("안녕");
    }

    @Test
    @DisplayName("postString: 본문과 Content-Type, 헤더를 보내고 응답 본문을 돌려준다")
    void postStringSendsBodyAndContentType() throws Exception {
        String response = client(2000, 0, 0)
                .postString(url("/echo"), "application/json", "{\"a\":\"한글\"}", "X-Test", "xyz");

        assertThat(response).isEqualTo("ok");
        assertThat(seenContentType.get()).isEqualTo("application/json");
        assertThat(seenHeader.get()).isEqualTo("xyz");
        assertThat(seenBody.get()).isEqualTo("{\"a\":\"한글\"}");
    }

    @Test
    @DisplayName("postString: 200이 아니면 CrawlHttpException")
    void postStringThrowsOnNon200() {
        assertThatThrownBy(() -> client(2000, 0, 0).postString(url("/missing"), "text/plain", "x"))
                .isInstanceOf(CrawlHttpException.class);
    }

    @Nested
    @DisplayName("CrawlSupport")
    class CrawlSupportTests {

        @Test
        @DisplayName("cleanText: 태그 제거, 엔티티 해제, 공백 정리, null은 빈 문자열")
        void cleanText() {
            assertThat(CrawlerUtil.cleanText("<p>할인 &amp;  <b>이벤트</b></p>\n 끝")).isEqualTo("할인 & 이벤트 끝");
            assertThat(CrawlerUtil.cleanText(null)).isEmpty();
        }

        @Test
        @DisplayName("absoluteUrl: //, http, /경로, 상대경로를 모두 절대 URL로 만든다")
        void absoluteUrl() {
            String base = "https://example.com";
            assertThat(CrawlerUtil.absoluteUrl(base, "//cdn.example.com/a.png")).isEqualTo("https://cdn.example.com/a.png");
            assertThat(CrawlerUtil.absoluteUrl(base, "http://other.com/a.png")).isEqualTo("http://other.com/a.png");
            assertThat(CrawlerUtil.absoluteUrl(base, "/img/a.png")).isEqualTo("https://example.com/img/a.png");
            assertThat(CrawlerUtil.absoluteUrl(base, "img/a.png")).isEqualTo("https://example.com/img/a.png");
            assertThat(CrawlerUtil.absoluteUrl(base, "")).isEmpty();
            assertThat(CrawlerUtil.absoluteUrl(base, null)).isEmpty();
        }

        @Test
        @DisplayName("날짜 도우미: 시작은 0시, 종료는 23:59:59, 종료일이 없으면 2099-12-31")
        void dates() {
            LocalDate date = LocalDate.of(2026, 10, 21);
            assertThat(CrawlerUtil.startOfDay(date)).isEqualTo(LocalDateTime.of(2026, 10, 21, 0, 0, 0));
            assertThat(CrawlerUtil.endOfDay(date)).isEqualTo(LocalDateTime.of(2026, 10, 21, 23, 59, 59));
            assertThat(CrawlerUtil.openEndedEnd()).isEqualTo(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
        }

        @Test
        @DisplayName("event: 설명과 이벤트 코드는 비운다")
        void event() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

            CreateEvent event = CrawlerUtil.event("제목", "https://e.com/1", "https://e.com/1.png", start, end, 7L, true);

            assertThat(event.title()).isEqualTo("제목");
            assertThat(event.description()).isNull();
            assertThat(event.url()).isEqualTo("https://e.com/1");
            assertThat(event.img()).isEqualTo("https://e.com/1.png");
            assertThat(event.startDate()).isEqualTo(start);
            assertThat(event.endDate()).isEqualTo(end);
            assertThat(event.brandId()).isEqualTo(7L);
            assertThat(event.isActive()).isTrue();
            assertThat(event.eventCodes()).isNull();
        }
    }
}
