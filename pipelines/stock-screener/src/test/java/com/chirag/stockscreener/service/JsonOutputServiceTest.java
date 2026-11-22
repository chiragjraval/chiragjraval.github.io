package com.chirag.stockscreener.service;

import com.chirag.stockscreener.model.CompanyAttributes;
import com.chirag.stockscreener.model.CompanyDetail;
import com.chirag.stockscreener.model.CompanyMetadata;
import com.chirag.stockscreener.model.CompanyScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonOutputServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDirectory;

    @Test
    void writesSummaryAndCompanyDetailWithCurrentJsonContract() throws Exception {
        CompanyDetail company = createCompany();

        JsonOutputService.outputToJson(List.of(company), tempDirectory.toString());

        JsonNode summary = objectMapper.readTree(tempDirectory.resolve("summary.json").toFile());
        JsonNode detail = objectMapper.readTree(
                tempDirectory.resolve("details").resolve("TEST.json").toFile());

        assertEquals(1, summary.path("totalCompanies").asInt());
        assertFalse(summary.path("generatedAt").asText().isBlank());
        assertEquals("TEST", summary.path("companies").get(0).path("companyCode").asText());
        assertEquals("TEST", detail.path("metadata").path("companyCode").asText());
        assertEquals("Test Company", detail.path("attributes").path("companyName").asText());
        assertEquals(10.0, detail.path("attributes").path("marketCap").asDouble(), 0.001);
        assertTrue(detail.has("quarterlyResults"));
        assertTrue(detail.has("technicalHistory"));
        assertEquals(13.0, detail.path("companyScore").path("totalScore").asDouble(), 0.001);
    }

    @Test
    void propagatesOutputDirectoryFailures() throws Exception {
        Path outputFile = Files.writeString(tempDirectory.resolve("not-a-directory"), "block");

        assertThrows(IOException.class,
                () -> JsonOutputService.outputToJson(List.of(), outputFile.toString()));
    }

    private CompanyDetail createCompany() {
        CompanyAttributes attributes = new CompanyAttributes();
        attributes.setCompanyCode("TEST");
        attributes.setCompanyName("Test Company");
        attributes.setMarketCap(10.0);
        attributes.setPeRatio(5.0);

        CompanyScore score = new CompanyScore.Builder()
                .setPriceHigherThanDma50(5.0)
                .setSalesGrowthPercentile(8.0)
                .build();

        CompanyMetadata metadata = new CompanyMetadata("1", "TEST", "Test Company", "/company/TEST/");
        metadata.setAttributes(attributes);
        metadata.setCompanyScore(score);

        CompanyDetail company = new CompanyDetail();
        company.setMetadata(metadata);
        company.setAttributes(attributes);
        company.setCompanyScore(score);
        return company;
    }
}
