package com.chirag.nhbresidex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndexObservation(LocalDate date, BigDecimal value) {
}
