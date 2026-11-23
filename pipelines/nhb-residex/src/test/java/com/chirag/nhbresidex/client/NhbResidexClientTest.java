package com.chirag.nhbresidex.client;

import com.chirag.nhbresidex.model.QuarterObservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NhbResidexClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesQuarterRowsFromJsonEncodedTrendResponse() throws Exception {
        List<QuarterObservation> observations = NhbResidexClient.parseTrendResponse(
                fixture("nhb/trend-2024.json"), "2024-25");

        assertEquals(2, observations.size());
        assertEquals("2024-25", observations.get(0).baseYear());
        assertEquals("2025-06-30", observations.get(0).quarterEnd().toString());
        assertEquals("5473.00", observations.get(0).weightedAverageComposite().toPlainString());
        assertEquals("101.63", observations.get(0).hfcIndex().toPlainString());
    }

    @Test
    void parsesCatalogAndAllThreeBaseYearFixtures() throws Exception {
        assertEquals("Chakan", NhbResidexClient.parseCatalog(
                fixture("nhb/cities-maharashtra.json")).getFirst().name());
        assertEquals("2017-18", NhbResidexClient.parseTrendResponse(
                fixture("nhb/trend-2018.json"), "2017-18").getFirst().baseYear());
        assertEquals("2013-06-30", NhbResidexClient.parseTrendResponse(
                fixture("nhb/trend-2013.json"), "2012-13").getFirst().quarterEnd().toString());
    }

    @Test
    void permitsMissingHfcIndexButRejectsWrongPriceMethod() throws Exception {
        List<QuarterObservation> observations = NhbResidexClient.parseTrendResponse(
                "{\"status\":1,\"data\":\"[{\\\"QTR_YEAR\\\":\\\"Mar-2018\\\",\\\"WTAVG_COMPOSITE\\\":\\\"4800\\\",\\\"HFC_INDEX\\\":null,\\\"PRICE_METHOD_ID\\\":2.0}]\"}",
                "2017-18");
        assertNull(observations.getFirst().hfcIndex());
        assertThrows(IOException.class, () -> NhbResidexClient.parseTrendResponse(
                "{\"status\":1,\"data\":\"[{\\\"QTR_YEAR\\\":\\\"Mar-2018\\\",\\\"WTAVG_COMPOSITE\\\":\\\"4800\\\",\\\"PRICE_METHOD_ID\\\":3}]\"}",
                "2017-18"));
    }

    @Test
    void rejectsFailedTrendResponses() throws Exception {
        assertThrows(IOException.class,
                () -> NhbResidexClient.parseTrendResponse(
                        "{\"status\":0,\"data\":\"[]\"}", "2024-25"));
    }

    @Test
    void skipsRowsWithoutWeightedPrice() throws Exception {
        List<QuarterObservation> observations = NhbResidexClient.parseTrendResponse(
                "{\"status\":1,\"data\":\"[{\\\"QTR_YEAR\\\":\\\"Jun-2025\\\",\\\"WTAVG_COMPOSITE\\\":null}]\"}",
                "2024-25",
                "/DashboardBY2024/Get_NHBResiCersai_PriceTrend");

        assertEquals(0, observations.size());
    }

    private String fixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test fixture: " + path);
            }
            return objectMapper.readTree(stream).toString();
        }
    }
}
