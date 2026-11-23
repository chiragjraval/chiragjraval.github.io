package com.chirag.nhbresidex.service;

import com.chirag.nhbresidex.model.BenchmarkDefinition;
import com.chirag.nhbresidex.model.CityRef;
import com.chirag.nhbresidex.model.IndexObservation;
import com.chirag.nhbresidex.model.QuarterObservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonOutputServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesContractAndSkipsUnchangedData() throws Exception {
        CityRef city = new CityRef("27", "Maharashtra", "131", "Chakan");
        BenchmarkDefinition benchmark = new BenchmarkDefinition("nifty-50", "NIFTY 50", "1272594");
        Map<CityRef, List<QuarterObservation>> cities = Map.of(city, List.of(
                new QuarterObservation(
                        LocalDate.parse("2025-06-30"),
                        "2024-25",
                        new BigDecimal("5477"),
                        new BigDecimal("6290"),
                        new BigDecimal("6010"),
                        new BigDecimal("5800"),
                        new BigDecimal("101.63"),
                        "NHB Residex"),
                new QuarterObservation(
                        LocalDate.parse("2025-09-30"),
                        "2024-25",
                        new BigDecimal("5520"),
                        new BigDecimal("6400"),
                        new BigDecimal("6100"),
                        new BigDecimal("5900"),
                        new BigDecimal("102.10"),
                        "NHB Residex")));
        Map<BenchmarkDefinition, List<IndexObservation>> benchmarks = Map.of(benchmark, List.of(
                new IndexObservation(LocalDate.parse("2025-06-27"), new BigDecimal("25000")),
                new IndexObservation(LocalDate.parse("2025-09-26"), new BigDecimal("27500"))));

        assertTrue(JsonOutputService.writeAll(cities, benchmarks, tempDirectory));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode manifest = mapper.readTree(tempDirectory.resolve("manifest.json").toFile());
        assertEquals(1, manifest.path("schemaVersion").asInt());
        assertEquals("INR per sq. ft.", manifest.path("cityPriceUnit").asText());
        assertEquals(1, manifest.path("cities").size());
        assertTrue(tempDirectory.resolve("cities/27-131.json").toFile().isFile());
        assertEquals("6290", mapper.readTree(tempDirectory.resolve("cities/27-131.json").toFile())
                .path("observations").get(0).path("weightedAverageLessThan60").asText());
        assertTrue(tempDirectory.resolve("benchmarks/nifty-50.json").toFile().isFile());
        String generatedAt = manifest.path("generatedAt").asText();

        Map<CityRef, List<QuarterObservation>> partialCityUpdate = Map.of(city, List.of(
                new QuarterObservation(
                        LocalDate.parse("2026-06-30"),
                        "2024-25",
                        new BigDecimal("5790"),
                        new BigDecimal("6700"),
                        new BigDecimal("6400"),
                        new BigDecimal("6200"),
                        new BigDecimal("107.10"),
                        "NHB Residex")));
        Map<BenchmarkDefinition, List<IndexObservation>> partialBenchmarkUpdate = Map.of(benchmark, List.of(
                new IndexObservation(LocalDate.parse("2026-01-02"), new BigDecimal("26328.55"))));

        assertTrue(JsonOutputService.writeAll(partialCityUpdate, partialBenchmarkUpdate, tempDirectory));
        assertEquals(3, mapper.readTree(
                tempDirectory.resolve("cities/27-131.json").toFile()).path("observations").size());
        assertEquals(3, mapper.readTree(
                tempDirectory.resolve("benchmarks/nifty-50.json").toFile()).path("observations").size());
        String updatedAt = mapper.readTree(tempDirectory.resolve("manifest.json").toFile())
                .path("generatedAt").asText();
        assertNotEquals(generatedAt, updatedAt);
        assertFalse(JsonOutputService.writeAll(partialCityUpdate, partialBenchmarkUpdate, tempDirectory));
        assertEquals(updatedAt, mapper.readTree(
                tempDirectory.resolve("manifest.json").toFile()).path("generatedAt").asText());
    }
}
