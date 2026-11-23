package com.chirag.nhbresidex.util;

import com.chirag.nhbresidex.client.ScreenerIndexClient;
import okhttp3.HttpUrl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class FixtureHttpTransport implements HttpTransport {
    private static final Set<String> BENCHMARK_IDS = ScreenerIndexClient.benchmarks().stream()
            .map(definition -> definition.screenerCompanyId())
            .collect(Collectors.toUnmodifiableSet());
    private final Path fixtureRoot;

    public FixtureHttpTransport(Path fixtureRoot) {
        this.fixtureRoot = fixtureRoot.toAbsolutePath().normalize();
    }

    @Override
    public String get(String url, Map<String, String> headers) throws IOException {
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) {
            throw new IOException("Invalid fixture request URL: " + url);
        }
        String path = parsed.encodedPath();
        if (path.endsWith("/ApplicationUtilities/GetState")) {
            return read("nhb/states.json");
        }
        if (path.endsWith("/ApplicationUtilities/GetCity")) {
            String stateId = parsed.queryParameter("State");
            if (stateId == null || !stateId.matches("[A-Za-z0-9_-]+")) {
                throw new IOException("Fixture request is missing a valid State parameter");
            }
            return read("nhb/cities-" + stateId + ".json");
        }
        String[] segments = path.split("/");
        if (path.contains("/api/company/") && segments.length > 3) {
            String benchmarkId = segments[3];
            if (BENCHMARK_IDS.contains(benchmarkId)) {
                return read("screener/" + benchmarkId + ".json");
            }
        }
        throw new IOException("No fixture for GET " + url);
    }

    @Override
    public String postForm(String url, Map<String, String> headers, Map<String, String> form)
            throws IOException {
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null || !parsed.encodedPath().endsWith("Get_NHBResiCersai_PriceTrend")) {
            throw new IOException("No fixture for POST " + url);
        }
        if (!"CITY".equals(form.get("reporttype"))
                || !"HFC".equals(form.get("lookup"))
                || !"2".equals(form.get("pricemtdid"))) {
            throw new IOException("Fixture request uses unsupported NHB report parameters");
        }

        String cityId = form.get("cityid");
        if (cityId == null || !cityId.matches("[A-Za-z0-9_-]+")) {
            throw new IOException("Fixture request is missing a valid cityid");
        }
        String baseYear;
        if (parsed.encodedPath().contains("DashboardBY2024")) {
            baseYear = "2024";
        } else if (parsed.encodedPath().contains("DashboardBY2018")) {
            baseYear = "2018";
        } else if (parsed.encodedPath().contains("DashboardBY2013")) {
            baseYear = "2013";
        } else {
            throw new IOException("No NHB base-year fixture for " + url);
        }
        return read("nhb/trend-" + cityId + "-" + baseYear + ".json");
    }

    private String read(String relativePath) throws IOException {
        Path fixture = fixtureRoot.resolve(relativePath).normalize();
        if (!fixture.startsWith(fixtureRoot) || !Files.isRegularFile(fixture)) {
            throw new IOException("Missing fixture: " + fixture);
        }
        return Files.readString(fixture, StandardCharsets.UTF_8);
    }
}
