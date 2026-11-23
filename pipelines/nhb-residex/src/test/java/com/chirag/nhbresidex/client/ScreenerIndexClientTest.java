package com.chirag.nhbresidex.client;

import com.chirag.nhbresidex.model.IndexObservation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenerIndexClientTest {

    @Test
    void readsOnlyWeeklyIndexPriceValues() throws Exception {
        String response = fixture("screener/nifty-50.json");

        List<IndexObservation> observations = ScreenerIndexClient.parseIndexResponse(response);

        assertEquals(2, observations.size());
        assertEquals("2019-01-04", observations.get(0).date().toString());
        assertEquals("10727.35", observations.get(0).value().toPlainString());
    }

    @Test
    void rejectsDailyOrMissingPriceSeries() {
        assertThrows(IOException.class,
                () -> ScreenerIndexClient.parseIndexResponse(
                        "{\"meta\":{\"is_weekly\":false},\"datasets\":[]}"));
    }

    @Test
    void acceptsMetricBasedWeeklyDataWithoutCadenceMetadata() throws Exception {
        String response = """
                {
                  "datasets": [{
                    "metric": "Index Price",
                    "label": "Index value",
                    "values": [["2026-11-06", "25000"], ["2026-11-13", "25100"]]
                  }]
                }
                """;

        List<IndexObservation> observations = ScreenerIndexClient.parseIndexResponse(response);

        assertEquals(2, observations.size());
        assertEquals("25100", observations.getLast().value().toPlainString());
    }

    @Test
    void rejectsDailyMetricBasedDataWhenCadenceMetadataIsMissing() {
        String response = """
                {
                  "datasets": [{
                    "metric": "Index Price",
                    "label": "Index value",
                    "values": [["2026-11-12", "25000"], ["2026-11-13", "25100"]]
                  }]
                }
                """;

        assertThrows(IOException.class, () -> ScreenerIndexClient.parseIndexResponse(response));
    }

    @Test
    void rejectsSinglePointDataWhenCadenceMetadataIsMissing() {
        String response = """
                {
                  "datasets": [{
                    "metric": "Index Price",
                    "label": "Index value",
                    "values": [["2026-11-13", "25100"]]
                  }]
                }
                """;

        assertThrows(IOException.class, () -> ScreenerIndexClient.parseIndexResponse(response));
    }

    private String fixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
