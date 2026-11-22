package com.chirag.stockscreener.enums;

public enum Environment {
    TEST(1),
    DEV(1),
    PROD(Integer.MAX_VALUE);

    private final Integer maxPagesToScan;
    private final String outputDirectory;

    private Environment(int maxPagesToScan) {
        this.maxPagesToScan = maxPagesToScan;
        this.outputDirectory = String.format("data/stock-screener/%s", this.name().toLowerCase());
    }

    public Integer getMaxPagesToScan() {
        return maxPagesToScan;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }
}
