package com.chirag.nhbresidex;

import com.chirag.nhbresidex.client.NhbResidexClient;
import com.chirag.nhbresidex.client.ScreenerIndexClient;
import com.chirag.nhbresidex.model.BenchmarkDefinition;
import com.chirag.nhbresidex.model.CityRef;
import com.chirag.nhbresidex.model.IndexObservation;
import com.chirag.nhbresidex.model.QuarterObservation;
import com.chirag.nhbresidex.service.JsonOutputService;
import com.chirag.nhbresidex.util.FixtureHttpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixturePipelineTest {

    @TempDir
    Path outputDirectory;

    @Test
    void runsAllAdaptersAndWritesTheComparisonDatasetWithoutNetworkAccess() throws Exception {
        FixtureHttpTransport transport = new FixtureHttpTransport(Path.of("sample-data"));
        Map<CityRef, List<QuarterObservation>> cities =
                new NhbResidexClient(transport).fetchAllCities(1);
        Map<BenchmarkDefinition, List<IndexObservation>> benchmarks =
                new ScreenerIndexClient(transport).fetchAll();

        assertEquals(1, cities.size());
        assertEquals(4, benchmarks.size());
        assertEquals("5477", cities.values().iterator().next().stream()
                .filter(value -> value.quarterEnd().toString().equals("2025-06-30"))
                .findFirst().orElseThrow().weightedAverageComposite().toPlainString());

        assertTrue(JsonOutputService.writeAll(cities, benchmarks, outputDirectory));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode manifest = mapper.readTree(outputDirectory.resolve("manifest.json").toFile());
        assertEquals(1, manifest.path("cities").size());
        assertEquals(4, manifest.path("benchmarks").size());
        assertEquals(4, mapper.readTree(
                outputDirectory.resolve("benchmarks/nifty-smallcap-250.json").toFile())
                .path("observations").size());
    }
}
