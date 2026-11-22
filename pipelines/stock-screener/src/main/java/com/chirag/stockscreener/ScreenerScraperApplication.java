package com.chirag.stockscreener;

import com.chirag.stockscreener.context.ExecutionContext;
import com.chirag.stockscreener.context.ExecutionContextImpl;
import com.chirag.stockscreener.extractor.company.CompanyDetailExtractor;
import com.chirag.stockscreener.extractor.query.QueryResultsExtractor;
import com.chirag.stockscreener.model.CompanyDetail;
import com.chirag.stockscreener.model.CompanyMetadata;
import com.chirag.stockscreener.service.JsonOutputService;
import com.chirag.stockscreener.util.HttpClientUtil;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Main scraper application for screener.in data extraction
 * Runs during the Maven test phase when invoked through the configured lifecycle
 */
public class ScreenerScraperApplication {

    private static final Logger logger = LoggerFactory.getLogger(ScreenerScraperApplication.class);
    private static final HttpClientUtil httpClientUtil = new HttpClientUtil(5, 2000, 60000);

    public static void main(String[] args) {
        logger.info("Starting Screener.in Data Scraper Application");
        ExecutionContext executionContext;

        try {
            executionContext = new ExecutionContextImpl(args);
            logger.info("ExecutionContext = {}", executionContext);
        }
        catch (ParseException | IOException e) {
            logger.error("Error initializing execution context", e);
            throw new IllegalStateException("Error initializing execution context", e);
        }

        QueryResultsExtractor queryResultsExtractor = new QueryResultsExtractor(executionContext, httpClientUtil);
        CompanyDetailExtractor companyDetailExtractor = new CompanyDetailExtractor(httpClientUtil);

        try {
            // Step 1: Parse screener list to get company metadata
            logger.info("Step 1: Fetching company list from screener...");
            QueryResultsExtractor.QueryResults queryResults = queryResultsExtractor.apply(executionContext.getScreenerQueryLink());

            if (queryResults.companyMetadataMap().isEmpty()) {
                throw new IllegalStateException("No companies found in screener list");
            }

            logger.info("Found {} companies in screener list", queryResults.companyMetadataMap().size());

            // Step 2: Fetch detailed information for each company
            logger.info("Step 2: Fetching detailed information for each company...");
            List<CompanyDetail> details = queryResults.companyMetadataMap().keySet().stream().sorted().map(companyCode -> {
                CompanyMetadata metadata = queryResults.companyMetadataMap().get(companyCode);
                CompanyDetail detail = companyDetailExtractor.apply(metadata);
                metadata.setAttributes(detail.getAttributes());
                metadata.setCompanyScore(detail.getCompanyScore());
                return detail;
            }).toList();
            logger.info("Successfully processed {} companies", details.size());

            // Step 3: Output results to JSON files
            logger.info("Step 3: Writing results to JSON files...");
            JsonOutputService.outputToJson(details, executionContext.getOutputDirectory());

            logger.info("Screener Data Scraper Application completed successfully");

        } catch (Exception e) {
            logger.error("Fatal error in scraper application", e);
            throw new IllegalStateException("Fatal error in scraper application", e);
        }
    }
}

