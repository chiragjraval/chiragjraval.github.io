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

    private QuarterObservation observation(String baseYear, String date, String price) {
        return new QuarterObservation(
                LocalDate.parse(date),
                baseYear,
                new BigDecimal(price),
                BigDecimal.ONE,
                "fixture");
    }
}
