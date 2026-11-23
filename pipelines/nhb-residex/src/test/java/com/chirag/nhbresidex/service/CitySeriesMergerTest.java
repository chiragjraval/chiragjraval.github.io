package com.chirag.nhbresidex.service;

import com.chirag.nhbresidex.model.QuarterObservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CitySeriesMergerTest {

    @Test
    void prefersNewerBaseYearForOverlappingQuarterAndKeepsOlderHistory() {
        List<QuarterObservation> merged = CitySeriesMerger.merge(List.of(
                observation("2017-18", "2025-06-30", "5473"),
                observation("2024-25", "2025-06-30", "5477"),
                observation("2012-13", "2018-03-31", "4800")));

        assertEquals(2, merged.size());
        assertEquals("2024-25", merged.get(1).baseYear());
        assertEquals(new BigDecimal("5477"), merged.get(1).weightedAverageComposite());
    }

    @Test
    void fillsMissingFieldsFromOlderBaseYearInsteadOfDroppingThem() {
        List<QuarterObservation> merged = CitySeriesMerger.merge(List.of(
                new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2012-13", new BigDecimal("4853"),
                        new BigDecimal("4366"), new BigDecimal("4604"), new BigDecimal("5330"),
                        new BigDecimal("103"), "old"),
                new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2017-18", null,
                        new BigDecimal("4366"), new BigDecimal("4604"), new BigDecimal("5330"),
                        new BigDecimal("76"), "new")));

        assertEquals(1, merged.size());
        QuarterObservation value = merged.getFirst();
        assertEquals("2017-18", value.baseYear());
        assertEquals(new BigDecimal("4853"), value.weightedAverageComposite());
        assertEquals(new BigDecimal("76"), value.hfcIndex());
        assertEquals("new", value.sourceEndpoint());
    }

    @Test
    void prefersNewerBaseYearForFieldsPresentInBothRows() {
        List<QuarterObservation> merged = CitySeriesMerger.merge(List.of(
                new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2012-13", new BigDecimal("4853"),
                        new BigDecimal("4366"), new BigDecimal("4604"), new BigDecimal("5330"),
                        new BigDecimal("103"), "old"),
                new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2017-18", new BigDecimal("4900"),
                        new BigDecimal("4400"), new BigDecimal("4650"), new BigDecimal("5350"),
                        new BigDecimal("76"), "new")));

        assertEquals(1, merged.size());
        QuarterObservation value = merged.getFirst();
        assertEquals("2017-18", value.baseYear());
        assertEquals(new BigDecimal("4900"), value.weightedAverageComposite());
        assertEquals(new BigDecimal("4400"), value.weightedAverageLessThan60());
        assertEquals(new BigDecimal("76"), value.hfcIndex());
    }

    @Test
    void rejectsConflictingRowsFromSameBaseYear() {
        assertThrows(IllegalArgumentException.class, () -> CitySeriesMerger.merge(List.of(
                observation("2024-25", "2025-06-30", "5473"),
                observation("2024-25", "2025-06-30", "5477"))));
    }

    @Test
    void upsertKeepsOlderHistoryAndAllowsCorrectionsFromTheSameBaseYear() {
        List<QuarterObservation> merged = CitySeriesMerger.upsert(
                List.of(observation("2024-25", "2025-06-30", "5473")),
                List.of(
                        observation("2024-25", "2025-06-30", "5477"),
                        observation("2017-18", "2025-09-30", "5520")));

        assertEquals(2, merged.size());
        assertEquals(new BigDecimal("5477"), merged.getFirst().weightedAverageComposite());
        assertEquals("2024-25", merged.getFirst().baseYear());
    }

    @Test
    void upsertReplacesExistingRowsWithBackfilledCompositeFromOlderBaseYear() {
        List<QuarterObservation> merged = CitySeriesMerger.upsert(
                List.of(new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2017-18", null,
                        new BigDecimal("4366"), new BigDecimal("4604"), new BigDecimal("5330"),
                        new BigDecimal("76"), "fixture")),
                List.of(new QuarterObservation(
                        LocalDate.parse("2013-06-30"), "2017-18", new BigDecimal("4853"),
                        new BigDecimal("4366"), new BigDecimal("4604"), new BigDecimal("5330"),
                        new BigDecimal("76"), "fixture")));

        assertEquals(1, merged.size());
        assertEquals(new BigDecimal("4853"), merged.getFirst().weightedAverageComposite());
    }

    private QuarterObservation observation(String baseYear, String date, String price) {
        return new QuarterObservation(
                LocalDate.parse(date),
                baseYear,
                new BigDecimal(price),
                new BigDecimal(price).add(new BigDecimal("100")),
                new BigDecimal(price).add(new BigDecimal("200")),
                new BigDecimal(price).add(new BigDecimal("300")),
                BigDecimal.ONE,
                "fixture");
    }
}
