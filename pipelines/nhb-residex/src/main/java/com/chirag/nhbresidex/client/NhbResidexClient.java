package com.chirag.nhbresidex.client;

import com.chirag.nhbresidex.model.CityRef;
import com.chirag.nhbresidex.model.QuarterObservation;
import com.chirag.nhbresidex.service.CitySeriesMerger;
import com.chirag.nhbresidex.util.HttpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NhbResidexClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NhbResidexClient.class);
    private static final String BASE_URL = "https://residex.nhbonline.org.in";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; FinancialDataLab/1.0)";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<BaseYearEndpoint> BASE_YEAR_ENDPOINTS = List.of(
            new BaseYearEndpoint("2024-25", "/DashboardBY2024/Get_NHBResiCersai_PriceTrend", false),
            new BaseYearEndpoint("2017-18", "/DashboardBY2018/Get_NHBResiCersai_PriceTrend", true),
            new BaseYearEndpoint("2012-13", "/DashboardBY2013/Get_NHBResiCersai_PriceTrend", true));

    private final HttpTransport transport;

    public NhbResidexClient(HttpTransport transport) {
        this.transport = transport;
    }

    public Map<CityRef, List<QuarterObservation>> fetchAllCities() throws IOException {
        return fetchAllCities(0);
    }

    public Map<CityRef, List<QuarterObservation>> fetchAllCities(int maxCities) throws IOException {
        if (maxCities < 0) {
            throw new IllegalArgumentException("maxCities must not be negative");
        }
        List<CatalogEntry> states = parseCatalog(get(
                BASE_URL + "/ApplicationUtilities/GetState", headers(BASE_URL + "/DashboardBY2024/DashboardBY2024")));
        if (states.isEmpty()) {
            throw new IOException("NHB returned an empty state catalog");
        }

        Map<CityRef, List<QuarterObservation>> results = new LinkedHashMap<>();
        for (CatalogEntry state : states) {
            String cityUrl = HttpUrl.parse(BASE_URL + "/ApplicationUtilities/GetCity")
                    .newBuilder()
                    .addQueryParameter("State", state.id())
                    .build()
                    .toString();
            List<CatalogEntry> cities = parseCatalog(
                    get(cityUrl, headers(BASE_URL + "/DashboardBY2024/DashboardBY2024")));

            for (CatalogEntry city : cities) {
                if (maxCities > 0 && results.size() >= maxCities) {
                    break;
                }
                CityRef cityRef = new CityRef(state.id(), state.name(), city.id(), city.name());
                List<QuarterObservation> cityObservations = new ArrayList<>();
                for (BaseYearEndpoint endpoint : BASE_YEAR_ENDPOINTS) {
                    Map<String, String> form = new LinkedHashMap<>();
                    form.put("reporttype", "CITY");
                    form.put("lookup", "HFC");
                    form.put("cityid", city.id());
                    form.put("pincode", "0");
                    form.put("wardid", "0");
                    form.put("pricemtdid", "2");
                    form.put("locid", "0");
                    if (endpoint.includeEarlierHistory()) {
                        form.put("chkbefore1718", "1");
                    }

                    String pageName = endpoint.path().substring(1, endpoint.path().indexOf('/', 1));
                    String pageUrl = BASE_URL + "/" + pageName + "/" + pageName;
                    String response = postForm(
                            BASE_URL + endpoint.path(),
                            headers(pageUrl),
                            form);
                    try {
                        cityObservations.addAll(parseTrendResponse(
                                response, endpoint.baseYear(), endpoint.path(), cityRef));
                    } catch (IOException exception) {
                        throw new IOException(
                                "Failed to parse NHB data for " + cityRef + " base year "
                                        + endpoint.baseYear() + " from " + endpoint.path(),
                                exception);
                    }
                }

                List<QuarterObservation> merged = CitySeriesMerger.merge(cityObservations);
                if (merged.isEmpty()) {
                    throw new IOException("NHB returned no observations for city " + cityRef);
                }
                results.put(cityRef, merged);
            }
            if (maxCities > 0 && results.size() >= maxCities) {
                break;
            }
        }

        if (results.isEmpty()) {
            throw new IOException("NHB returned no city observations");
        }
        return results;
    }

    public static List<CatalogEntry> parseCatalog(String response) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(response);
        JsonNode entries = root.isArray() ? root : root.path("data");
        if (!entries.isArray()) {
            throw new IOException("Unexpected NHB catalog response shape");
        }

        List<CatalogEntry> catalog = new ArrayList<>();
        for (JsonNode entry : entries) {
            String id = text(entry, "value");
            String name = text(entry, "text");
            if (id.isBlank() || name.isBlank()) {
                throw new IOException("NHB catalog entry is missing value or text");
            }
            if (!id.matches("[A-Za-z0-9_-]+")) {
                throw new IOException("NHB catalog entry has an invalid identifier: " + id);
            }
            catalog.add(new CatalogEntry(id, name));
        }
        return catalog;
    }

    public static List<QuarterObservation> parseTrendResponse(String response, String baseYear)
            throws IOException {
        return parseTrendResponse(response, baseYear, "fixture");
    }

    public static List<QuarterObservation> parseTrendResponse(
            String response, String baseYear, String sourceEndpoint) throws IOException {
        return parseTrendResponse(response, baseYear, sourceEndpoint, null);
    }

    private static List<QuarterObservation> parseTrendResponse(
            String response, String baseYear, String sourceEndpoint, CityRef city) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(response);
        JsonNode status = root.path("status");
        if (!isSuccessful(status)) {
            throw new IOException("NHB trend request failed with status " + status);
        }

        JsonNode rows = root.path("data");
        if (rows.isTextual()) {
            rows = OBJECT_MAPPER.readTree(rows.asText());
        }
        if (!rows.isArray()) {
            throw new IOException("Unexpected NHB trend response data shape");
        }

        List<QuarterObservation> observations = new ArrayList<>();
        List<LocalDate> missingPriceQuarters = new ArrayList<>();
        for (JsonNode row : rows) {
            JsonNode methodId = row.get("PRICE_METHOD_ID");
            if (methodId != null && !methodId.isNull()
                    && decimal(methodId, "PRICE_METHOD_ID").compareTo(BigDecimal.valueOf(2)) != 0) {
                throw new IOException("Unexpected NHB price method: " + methodId.asText());
            }

            LocalDate quarterEnd = parseQuarterEnd(text(row, "QTR_YEAR"));
            BigDecimal composite = optionalDecimal(row.get("WTAVG_COMPOSITE"), "WTAVG_COMPOSITE");
            BigDecimal lessThan60 = optionalDecimal(row.get("WTAVG_LESSTHAN60"), "WTAVG_LESSTHAN60");
            BigDecimal from60To110 = optionalDecimal(row.get("WTAVG_60TO110"), "WTAVG_60TO110");
            BigDecimal greaterThan110 = optionalDecimal(row.get("WTAVG_GREATER110"), "WTAVG_GREATER110");
            if (composite == null && lessThan60 == null && from60To110 == null && greaterThan110 == null) {
                missingPriceQuarters.add(quarterEnd);
                continue;
            }
            validatePositive(composite, "WTAVG_COMPOSITE", quarterEnd);
            validatePositive(lessThan60, "WTAVG_LESSTHAN60", quarterEnd);
            validatePositive(from60To110, "WTAVG_60TO110", quarterEnd);
            validatePositive(greaterThan110, "WTAVG_GREATER110", quarterEnd);
            BigDecimal index = optionalDecimal(row.get("HFC_INDEX"), "HFC_INDEX");
            if (index != null && index.signum() <= 0) {
                throw new IOException("NHB index must be greater than zero for " + quarterEnd);
            }
            observations.add(new QuarterObservation(
                    quarterEnd, baseYear, composite, lessThan60, from60To110, greaterThan110, index, sourceEndpoint));
        }
        if (!missingPriceQuarters.isEmpty()) {
            LOGGER.warn(
                    "Skipped {} NHB row(s) without carpet-area prices for city {} base year {} from {}: {}",
                    missingPriceQuarters.size(),
                    city == null ? "unknown" : city.cityName(),
                    baseYear,
                    sourceEndpoint,
                    missingPriceQuarters);
        }
        return observations;
    }

    private static Map<String, String> headers(String referer) {
        return Map.of(
                "Accept", "application/json, text/javascript, */*; q=0.01",
                "User-Agent", USER_AGENT,
                "X-Requested-With", "XMLHttpRequest",
                "Referer", referer);
    }

    private String get(String url, Map<String, String> headers) throws IOException {
        return transport.get(url, headers);
    }

    private String postForm(String url, Map<String, String> headers, Map<String, String> form)
            throws IOException {
        return transport.postForm(url, headers, form);
    }

    private static boolean isSuccessful(JsonNode status) {
        return status.isBoolean() && status.asBoolean()
                || status.isNumber() && status.asInt() == 1
                || status.isTextual()
                && ("1".equals(status.asText()) || "success".equalsIgnoreCase(status.asText()));
    }

    private static String text(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IOException("Missing NHB field: " + field);
        }
        return value.asText().trim();
    }

    private static BigDecimal decimal(JsonNode node, String field) throws IOException {
        if (node == null || node.isNull()) {
            throw new IOException("Missing numeric NHB field: " + field);
        }
        try {
            return new BigDecimal(node.asText().replace(",", "").trim());
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid numeric NHB field " + field + ": " + node.asText(), exception);
        }
    }

    private static BigDecimal optionalDecimal(JsonNode node, String field) throws IOException {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return decimal(node, field);
    }

    private static void validatePositive(BigDecimal value, String field, LocalDate quarterEnd)
            throws IOException {
        if (value != null && value.signum() <= 0) {
            throw new IOException("NHB " + field + " must be greater than zero for " + quarterEnd);
        }
    }

    private static LocalDate parseQuarterEnd(String value) throws IOException {
        try {
            return validateQuarterEnd(LocalDate.parse(value), value);
        } catch (DateTimeParseException ignored) {
            for (String pattern : List.of("MMM-uuuu", "MMM uuuu", "uuuu-MMM", "uuuu-MM")) {
                try {
                    YearMonth month = YearMonth.parse(
                            value,
                            DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
                    return validateQuarterEnd(month.atEndOfMonth(), value);
                } catch (DateTimeParseException ignoredPattern) {
                    continue;
                }
            }
            throw new IOException("Unsupported NHB quarter value: " + value);
        }
    }

    private static LocalDate validateQuarterEnd(LocalDate date, String sourceValue) throws IOException {
        if (!List.of(3, 6, 9, 12).contains(date.getMonthValue())
                || date.getDayOfMonth() != date.lengthOfMonth()) {
            throw new IOException("NHB date is not a quarter end: " + sourceValue);
        }
        return date;
    }

    public record CatalogEntry(String id, String name) {
    }

    private record BaseYearEndpoint(String baseYear, String path, boolean includeEarlierHistory) {
    }
}
