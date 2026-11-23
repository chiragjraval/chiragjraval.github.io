package com.chirag.nhbresidex.util;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class OkHttpTransport implements HttpTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkHttpTransport.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 2_000;
    private static final long DEFAULT_MAX_BACKOFF_MILLIS = 60_000;
    private static final long DEFAULT_REQUEST_INTERVAL_MILLIS = 500;
    private static final long RETRY_JITTER_MILLIS = 500;

    private final OkHttpClient client;
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final long requestIntervalNanos;
    private final Object pacingLock = new Object();
    private boolean requestStarted;
    private long previousRequestAtNanos;

    public OkHttpTransport() {
        this(
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_INITIAL_BACKOFF_MILLIS,
                DEFAULT_MAX_BACKOFF_MILLIS,
                DEFAULT_REQUEST_INTERVAL_MILLIS);
    }

    OkHttpTransport(
            int maxAttempts,
            long initialBackoffMillis,
            long maxBackoffMillis,
            long requestIntervalMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (initialBackoffMillis < 0 || maxBackoffMillis < initialBackoffMillis) {
            throw new IllegalArgumentException("Backoff durations must be non-negative and max >= initial");
        }
        if (requestIntervalMillis < 0) {
            throw new IllegalArgumentException("requestIntervalMillis must not be negative");
        }
        this.maxAttempts = maxAttempts;
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.requestIntervalNanos = TimeUnit.MILLISECONDS.toNanos(requestIntervalMillis);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(45))
                .build();
    }

    @Override
    public String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder request = new Request.Builder().url(url).get();
        headers.forEach(request::header);
        return execute(request.build());
    }

    @Override
    public String postForm(String url, Map<String, String> headers, Map<String, String> form)
            throws IOException {
        FormBody.Builder body = new FormBody.Builder();
        form.forEach(body::add);
        Request.Builder request = new Request.Builder().url(url).post(body.build());
        headers.forEach(request::header);
        return execute(request.build());
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    private String execute(Request request) throws IOException {
        IOException lastFailure = null;
        long backoffMillis = initialBackoffMillis;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                pace();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return response.body().string();
                    }

                    int status = response.code();
                    lastFailure = new IOException("HTTP " + status + " from " + request.url());
                    if (status != 429 && status < 500) {
                        throw new NonRetryableHttpException(lastFailure.getMessage());
                    }
                }
            } catch (NonRetryableHttpException exception) {
                throw exception;
            } catch (IOException exception) {
                lastFailure = exception;
            }

            if (attempt < maxAttempts) {
                long jitterMillis = ThreadLocalRandom.current().nextLong(RETRY_JITTER_MILLIS);
                long retryDelayMillis = Math.min(backoffMillis + jitterMillis, maxBackoffMillis);
                LOGGER.warn(
                        "HTTP request attempt {}/{} failed for {}; retrying after {} ms: {}",
                        attempt, maxAttempts, request.url(), retryDelayMillis, lastFailure.getMessage());
                sleep(retryDelayMillis, request.url().toString());
                backoffMillis = Math.min(backoffMillis * 2, maxBackoffMillis);
            }
        }

        throw new IOException(
                "Request failed after " + maxAttempts + " attempts: " + request.url(),
                lastFailure);
    }

    private void pace() throws IOException {
        synchronized (pacingLock) {
            long now = System.nanoTime();
            long waitNanos = previousRequestAtNanos + requestIntervalNanos - now;
            while (requestStarted && waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while pacing request");
                }
                now = System.nanoTime();
                waitNanos = previousRequestAtNanos + requestIntervalNanos - now;
            }
            previousRequestAtNanos = System.nanoTime();
            requestStarted = true;
        }
    }

    private static void sleep(long millis, String requestUrl) throws IOException {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrying " + requestUrl, exception);
        }
    }

    private static final class NonRetryableHttpException extends IOException {
        private NonRetryableHttpException(String message) {
            super(message);
        }
    }
}
