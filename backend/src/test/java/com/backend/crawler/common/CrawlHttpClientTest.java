package com.backend.crawler.common;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlHttpClientTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> seenUserAgent = new AtomicReference<>();

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
}