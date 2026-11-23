package com.chirag.nhbresidex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuarterObservation(
        LocalDate quarterEnd,
        String baseYear,
        BigDecimal weightedAverageComposite,
        BigDecimal hfcIndex,
        String sourceEndpoint) {
}
