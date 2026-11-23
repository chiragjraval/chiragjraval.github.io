package com.chirag.nhbresidex.client;

import com.chirag.nhbresidex.model.BenchmarkDefinition;
import com.chirag.nhbresidex.model.IndexObservation;
import com.chirag.nhbresidex.util.HttpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ScreenerIndexClient {
    private static final String BASE_URL = "https://www.screener.in/api/company/";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; FinancialDataLab/1.0)";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<BenchmarkDefinition> BENCHMARKS = List.of(
            new BenchmarkDefinition("nifty-50", "NIFTY 50", "1272594"),
            new BenchmarkDefinition("nifty-500", "NIFTY 500", "1272615"),
            new BenchmarkDefinition("nifty-midcap-150", "NIFTY MIDCAP 150", "1275143"),
            new BenchmarkDefinition("nifty-smallcap-250", "NIFTY SMALLCAP 250", "1275142"));

    private final HttpTransport transport;

    public ScreenerIndexClient(HttpTransport transport) {
        this.transport = transport;
    }

    public Map<BenchmarkDefinition, List<IndexObservation>> fetchAll() throws IOException {
        Map<BenchmarkDefinition, List<IndexObservation>> result = new LinkedHashMap<>();
        for (BenchmarkDefinition benchmark : BENCHMARKS) {
            String url = HttpUrl.parse(BASE_URL + benchmark.screenerCompanyId() + "/chart/")
                    .newBuilder()
                    .addQueryParameter("q", "Index Price-Index DMA50-Index DMA200")
                    .addQueryParameter("days", "10000")
                    .build()
                    .toString();
            String response = transport.get(url, Map.of(
                    "Accept", "application/json, text/javascript, */*; q=0.01",
                    "User-Agent", USER_AGENT,
                    "X-Requested-With", "XMLHttpRequest",
                    "Referer", referer(benchmark)));
            result.put(benchmark, parseIndexResponse(response));
        }
        return result;
    }

    public static List<IndexObservation> parseIndexResponse(String response) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(response);
        JsonNode weeklyMetadata = root.path("meta").get("is_weekly");
        if (weeklyMetadata != null && !weeklyMetadata.asBoolean(false)) {
            throw new IOException("Screener index response is not weekly");
        }

        JsonNode datasets = root.path("datasets");
        if (!datasets.isArray()) {
            throw new IOException("Unexpected Screener chart response shape");
        }
        JsonNode priceDataset = null;
        for (JsonNode dataset : datasets) {
            String metric = dataset.path("metric").asText();
            String label = dataset.path("label").asText();
            if ("Index Price".equals(metric)
                    || (metric.isBlank() && "Index Price".equals(label))) {
                priceDataset = dataset;
                break;
            }
        }
        if (priceDataset == null || !priceDataset.path("values").isArray()) {
            throw new IOException("Screener response is missing the Index Price dataset");
        }

        Map<LocalDate, BigDecimal> byDate = new TreeMap<>();
        for (JsonNode pair : priceDataset.path("values")) {
            if (!pair.isArray() || pair.size() != 2) {
                throw new IOException("Invalid Screener Index Price row: " + pair);
            }
            LocalDate date;
            BigDecimal value;
            try {
                if (looksLikeDate(pair.get(0).asText())) {
                    date = LocalDate.parse(pair.get(0).asText());
                    value = new BigDecimal(pair.get(1).asText().replace(",", ""));
                } else {
                    value = new BigDecimal(pair.get(0).asText().replace(",", ""));
                    date = LocalDate.parse(pair.get(1).asText());
                }
            } catch (RuntimeException exception) {
                throw new IOException("Invalid Screener index value pair: " + pair, exception);
            }
            if (value.signum() <= 0) {
                throw new IOException("Screener index value must be positive for " + date);
            }
            BigDecimal previous = byDate.putIfAbsent(date, value);
            if (previous != null && previous.compareTo(value) != 0) {
                throw new IOException("Conflicting Screener index values for " + date);
            }
        }
        if (byDate.isEmpty()) {
            throw new IOException("Screener returned no weekly Index Price values");
        }
        if (weeklyMetadata == null && byDate.size() < 2) {
            throw new IOException("Screener returned too few dated values to verify weekly cadence");
        }
        LocalDate previousDate = null;
        for (LocalDate date : byDate.keySet()) {
            if (previousDate != null && date.toEpochDay() - previousDate.toEpochDay() < 5) {
                throw new IOException("Screener index observations are not weekly");
            }
            previousDate = date;
        }
        return byDate.entrySet().stream()
                .map(entry -> new IndexObservation(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(IndexObservation::date))
                .toList();
    }

    public static List<BenchmarkDefinition> benchmarks() {
        return BENCHMARKS;
    }

    private static boolean looksLikeDate(String value) {
        return value.length() >= 10 && value.charAt(4) == '-' && value.charAt(7) == '-';
    }

    private static String referer(BenchmarkDefinition benchmark) {
        String page = switch (benchmark.id()) {
            case "nifty-50" -> "NIFTY";
            case "nifty-500" -> "CNX500";
            case "nifty-midcap-150" -> "NMIDCAP150";
            case "nifty-smallcap-250" -> "SMALLCA250";
            default -> throw new IllegalArgumentException("Unknown NIFTY benchmark: " + benchmark.id());
        };
        return "https://www.screener.in/company/" + page + "/";
    }
}
