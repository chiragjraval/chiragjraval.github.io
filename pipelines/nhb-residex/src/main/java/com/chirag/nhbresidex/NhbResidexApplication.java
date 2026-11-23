package com.chirag.nhbresidex;

import com.chirag.nhbresidex.client.NhbResidexClient;
import com.chirag.nhbresidex.client.ScreenerIndexClient;
import com.chirag.nhbresidex.model.BenchmarkDefinition;
import com.chirag.nhbresidex.model.CityRef;
import com.chirag.nhbresidex.model.IndexObservation;
import com.chirag.nhbresidex.model.QuarterObservation;
import com.chirag.nhbresidex.service.JsonOutputService;
import com.chirag.nhbresidex.util.FixtureHttpTransport;
import com.chirag.nhbresidex.util.HttpTransport;
import com.chirag.nhbresidex.util.OkHttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class NhbResidexApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(NhbResidexApplication.class);

    private NhbResidexApplication() {
    }

    public static void main(String[] args) throws IOException {
        Options options = Options.parse(args);
        if (options.help()) {
            System.out.println(Options.usage());
            return;
        }
        if ("PROD".equals(options.environment()) && !options.confirmProduction()) {
            throw new IllegalArgumentException(
                    "Production output requires --confirm-prod; this does not authorize publishing.");
        }
        if ("PROD".equals(options.environment()) && options.fixtures()) {
            throw new IllegalArgumentException("PROD requires --live; fixture data cannot be written as production.");
        }
        if (options.fixtures() && !"TEST".equals(options.environment())) {
            throw new IllegalArgumentException("Fixtures can only be written to the TEST environment");
        }

        Path outputDirectory = Path.of(
                "data", "nhb-residex", options.environment().toLowerCase());
        LOGGER.info("Starting NHB Residex pipeline for {} into {}",
                options.environment(), outputDirectory);

        try (HttpTransport transport = options.fixtures()
                ? new FixtureHttpTransport(Path.of("pipelines", "nhb-residex", "sample-data"))
                : new OkHttpTransport()) {
            NhbResidexClient nhbClient = new NhbResidexClient(transport);
            ScreenerIndexClient indexClient = new ScreenerIndexClient(transport);
            Map<CityRef, List<QuarterObservation>> cities = nhbClient.fetchAllCities(options.maxCities());
            Map<BenchmarkDefinition, List<IndexObservation>> benchmarks = indexClient.fetchAll();
            boolean changed = JsonOutputService.writeAll(cities, benchmarks, outputDirectory);

            LOGGER.info("NHB Residex pipeline completed: {} cities, {} benchmarks, output changed={}",
                    cities.size(), benchmarks.size(), changed);
        }
    }

    private record Options(
            String environment, int maxCities, boolean confirmProduction, boolean help, boolean fixtures) {
        private static Options parse(String[] args) {
            String environment = "TEST";
            int maxCities = 1;
            boolean confirmProduction = false;
            boolean help = false;
            boolean maxCitiesSpecified = false;
            boolean fixtures = true;

            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "-e", "--environment" -> {
                        String option = args[index];
                        environment = requireValue(args, index + 1, option).toUpperCase();
                        index++;
                        if (!List.of("TEST", "DEV", "PROD").contains(environment)) {
                            throw new IllegalArgumentException("Environment must be TEST, DEV, or PROD");
                        }
                        if ("PROD".equals(environment) && !maxCitiesSpecified) {
                            maxCities = 0;
                        }
                    }
                    case "--max-cities" -> {
                        String value = requireValue(args, index + 1, "--max-cities");
                        index++;
                        try {
                            maxCities = Integer.parseInt(value);
                        } catch (NumberFormatException exception) {
                            throw new IllegalArgumentException("--max-cities must be a positive integer", exception);
                        }
                        if (maxCities < 1) {
                            throw new IllegalArgumentException("--max-cities must be a positive integer");
                        }
                        maxCitiesSpecified = true;
                    }
                    case "--confirm-prod" -> confirmProduction = true;
                    case "--fixtures" -> fixtures = true;
                    case "--live" -> fixtures = false;
                    case "-h", "--help" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown option: " + args[index]);
                }
            }
            return new Options(environment, maxCities, confirmProduction, help, fixtures);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static String usage() {
            return """
                    Usage: NhbResidexApplication [-e TEST|DEV|PROD] [--fixtures|--live]
                           [--max-cities N] [--confirm-prod]
                      Fixture mode is the default and writes synthetic data to TEST.
                      --live fetches source APIs; TEST and DEV default to one city.
                      PROD requires --live and --confirm-prod. Nothing publishes or deploys.
                    """;
        }
    }
}
