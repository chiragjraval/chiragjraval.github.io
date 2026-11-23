package com.chirag.nhbresidex.service;

import com.chirag.nhbresidex.model.QuarterObservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public final class CitySeriesMerger {
    private static final Map<String, Integer> BASE_YEAR_PRIORITY = Map.of(
            "2012-13", 1,
            "2017-18", 2,
            "2024-25", 3);

    private CitySeriesMerger() {
    }

    public static java.util.List<QuarterObservation> merge(
            Collection<QuarterObservation> observations) {
        Map<LocalDate, QuarterObservation> byQuarter = new TreeMap<>();
        for (QuarterObservation candidate : observations) {
            Integer candidatePriority = BASE_YEAR_PRIORITY.get(candidate.baseYear());
            if (candidatePriority == null) {
                throw new IllegalArgumentException("Unsupported NHB base year: " + candidate.baseYear());
            }

            QuarterObservation current = byQuarter.get(candidate.quarterEnd());
            if (current == null) {
                byQuarter.put(candidate.quarterEnd(), candidate);
                continue;
            }

            int currentPriority = BASE_YEAR_PRIORITY.get(current.baseYear());
            if (candidatePriority > currentPriority) {
                byQuarter.put(candidate.quarterEnd(), candidate);
            } else if (candidatePriority.equals(currentPriority)
                    && !candidate.equals(current)) {
                throw new IllegalArgumentException(
                        "Conflicting observations for " + candidate.quarterEnd()
                                + " from base year " + candidate.baseYear());
            }
        }

        return byQuarter.values().stream()
                .sorted(Comparator.comparing(QuarterObservation::quarterEnd))
                .toList();
    }

    public static java.util.List<QuarterObservation> upsert(
            Collection<QuarterObservation> existing,
            Collection<QuarterObservation> incoming) {
        Map<LocalDate, QuarterObservation> byQuarter = new TreeMap<>();
        for (QuarterObservation observation : merge(existing)) {
            byQuarter.put(observation.quarterEnd(), observation);
        }

        for (QuarterObservation observation : merge(incoming)) {
            QuarterObservation current = byQuarter.get(observation.quarterEnd());
            if (current == null
                    || BASE_YEAR_PRIORITY.get(observation.baseYear())
                    >= BASE_YEAR_PRIORITY.get(current.baseYear())) {
                byQuarter.put(observation.quarterEnd(), observation);
            }
        }

        return byQuarter.values().stream()
                .sorted(Comparator.comparing(QuarterObservation::quarterEnd))
                .toList();
    }
}
