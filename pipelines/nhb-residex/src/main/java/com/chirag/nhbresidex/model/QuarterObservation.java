package com.chirag.nhbresidex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuarterObservation(
        LocalDate quarterEnd,
        String baseYear,
        BigDecimal weightedAverageComposite,
        BigDecimal weightedAverageLessThan60,
        BigDecimal weightedAverage60To110,
        BigDecimal weightedAverageGreaterThan110,
        BigDecimal hfcIndex,
        String sourceEndpoint) {
}
