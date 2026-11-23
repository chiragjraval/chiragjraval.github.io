package com.chirag.nhbresidex.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OkHttpTransportTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void retriesRateLimitedResponsesAndReturnsSuccessfulBody() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/rate-limited", exchange -> {
            int attempt = attempts.incrementAndGet();
            respond(exchange, attempt < 3 ? 429 : 200, attempt < 3 ? "rate limited" : "ok");
        });
        server.start();

        try (OkHttpTransport transport = new OkHttpTransport(3, 0, 0, 0)) {
            assertEquals("ok", transport.get(baseUrl + "/rate-limited", Map.of()));
        }
        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryOtherClientErrors() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/bad-request", exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 400, "bad request");
        });
        server.start();

        try (OkHttpTransport transport = new OkHttpTransport(3, 0, 0, 0)) {
            assertThrows(IOException.class, () -> transport.get(baseUrl + "/bad-request", Map.of()));
        }
        assertEquals(1, attempts.get());
    }

    @Test
    void spacesRequestsByTheConfiguredMinimumInterval() throws Exception {
        List<Long> requestTimes = new CopyOnWriteArrayList<>();
        server.createContext("/paced", exchange -> {
            requestTimes.add(System.nanoTime());
            respond(exchange, 200, "ok");
        });
        server.start();

        try (OkHttpTransport transport = new OkHttpTransport(1, 0, 0, 50)) {
            transport.get(baseUrl + "/paced", Map.of());
            transport.get(baseUrl + "/paced", Map.of());
        }

        long intervalMillis = TimeUnit.NANOSECONDS.toMillis(requestTimes.get(1) - requestTimes.get(0));
        assertTrue(intervalMillis >= 40, "expected at least 40ms between requests, got " + intervalMillis);
    }

    private static void respond(
            com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, content.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(content);
        }
    }
}
