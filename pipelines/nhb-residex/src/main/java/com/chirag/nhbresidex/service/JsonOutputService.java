package com.chirag.nhbresidex.service;

import com.chirag.nhbresidex.model.BenchmarkDefinition;
import com.chirag.nhbresidex.model.CityRef;
import com.chirag.nhbresidex.model.IndexObservation;
import com.chirag.nhbresidex.model.QuarterObservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class JsonOutputService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonOutputService() {
    }

    public static boolean writeAll(
            Map<CityRef, List<QuarterObservation>> cities,
            Map<BenchmarkDefinition, List<IndexObservation>> benchmarks,
            Path outputDirectory) throws IOException {
        if (cities.isEmpty() || benchmarks.isEmpty()) {
            throw new IllegalArgumentException("NHB city and benchmark data must both be non-empty");
        }

        Files.createDirectories(outputDirectory.resolve("cities"));
        Files.createDirectories(outputDirectory.resolve("benchmarks"));
        Map<String, byte[]> cityFiles = new TreeMap<>();
        List<CityManifest> cityManifest = new ArrayList<>();
        for (Map.Entry<CityRef, List<QuarterObservation>> entry : cities.entrySet().stream()
                .sorted(Comparator.comparing(value -> value.getKey().fileId()))
                .toList()) {
            CityRef city = entry.getKey();
            validateCity(city, entry.getValue());
            String file = city.fileId() + ".json";
            Path cityPath = outputDirectory.resolve("cities").resolve(file);
            List<QuarterObservation> observations = readExistingCity(cityPath)
                    .map(previous -> CitySeriesMerger.upsert(previous.observations(), entry.getValue()))
                    .orElseGet(() -> CitySeriesMerger.merge(entry.getValue()));
            CityFile content = new CityFile(city, "INR per sq. ft.", "NHB Residex", observations);
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(content);
            cityFiles.put(file, bytes);
            cityManifest.add(new CityManifest(
                    city.stateId(), city.stateName(), city.cityId(), city.cityName(),
                    "cities/" + file, sha256(bytes)));
        }

        Map<String, byte[]> benchmarkFiles = new TreeMap<>();
        List<BenchmarkManifest> benchmarkManifest = new ArrayList<>();
        for (Map.Entry<BenchmarkDefinition, List<IndexObservation>> entry : benchmarks.entrySet().stream()
                .sorted(Comparator.comparing(value -> value.getKey().id()))
                .toList()) {
            BenchmarkDefinition benchmark = entry.getKey();
            List<IndexObservation> observations = mergeIndexObservations(
                    readExistingBenchmark(outputDirectory.resolve("benchmarks")
                            .resolve(benchmark.id() + ".json"))
                            .map(BenchmarkFile::observations)
                            .orElseGet(List::of),
                    entry.getValue());
            validateBenchmark(benchmark, observations);
            String file = benchmark.id() + ".json";
            BenchmarkFile content = new BenchmarkFile(
                    benchmark.id(), benchmark.name(), "Index points", "Screener.in", observations);
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(content);
            benchmarkFiles.put(file, bytes);
            benchmarkManifest.add(new BenchmarkManifest(
                    benchmark.id(), benchmark.name(), "Index points",
                    observations.getFirst().date().toString(),
                    observations.getLast().date().toString(),
                    observations.size(), "benchmarks/" + file, sha256(bytes)));
        }

        boolean filesChanged = writeFiles(outputDirectory.resolve("cities"), cityFiles);
        filesChanged |= writeFiles(outputDirectory.resolve("benchmarks"), benchmarkFiles);

        ManifestContent content = new ManifestContent(
                1,
                "INR per sq. ft.",
                cityManifest,
                benchmarkManifest,
                List.of("NHB Residex", "Screener.in"));
        Path manifestPath = outputDirectory.resolve("manifest.json");
        JsonNode existing = Files.exists(manifestPath)
                ? OBJECT_MAPPER.readTree(manifestPath.toFile())
                : null;
        ObjectNode semanticContent = OBJECT_MAPPER.valueToTree(content);
        if (existing instanceof ObjectNode existingObject) {
            existingObject.remove("generatedAt");
            if (existingObject.equals(semanticContent)) {
                boolean staleRemoved = removeStaleFiles(
                        outputDirectory.resolve("cities"), cityFiles.keySet());
                staleRemoved |= removeStaleFiles(
                        outputDirectory.resolve("benchmarks"), benchmarkFiles.keySet());
                return filesChanged || staleRemoved;
            }
        }

        Manifest manifest = new Manifest(
                content.schemaVersion(),
                content.cityPriceUnit(),
                content.cities(),
                content.benchmarks(),
                content.sources(),
                Instant.now().toString());
        byte[] manifestBytes = OBJECT_MAPPER.writeValueAsBytes(manifest);
        boolean manifestChanged = writeIfChanged(manifestPath, manifestBytes);
        boolean staleRemoved = removeStaleFiles(outputDirectory.resolve("cities"), cityFiles.keySet());
        staleRemoved |= removeStaleFiles(outputDirectory.resolve("benchmarks"), benchmarkFiles.keySet());
        return filesChanged || manifestChanged || staleRemoved;
    }

    private static void validateCity(CityRef city, List<QuarterObservation> observations) {
        if (city == null || !city.fileId().matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid NHB city identifier");
        }
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("NHB city must have at least one observation: " + city);
        }
    }

    private static void validateBenchmark(
            BenchmarkDefinition benchmark, List<IndexObservation> observations) {
        if (benchmark == null || !benchmark.id().matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Invalid benchmark identifier");
        }
        if (observations == null || observations.isEmpty()) {
            throw new IllegalArgumentException("Benchmark must have at least one observation: " + benchmark);
        }
    }

    private static boolean writeFiles(Path directory, Map<String, byte[]> files) throws IOException {
        boolean changed = false;
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            changed |= writeIfChanged(directory.resolve(file.getKey()), file.getValue());
        }
        return changed;
    }

    private static java.util.Optional<CityFile> readExistingCity(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(OBJECT_MAPPER.readValue(path.toFile(), CityFile.class));
    }

    private static java.util.Optional<BenchmarkFile> readExistingBenchmark(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(OBJECT_MAPPER.readValue(path.toFile(), BenchmarkFile.class));
    }

    private static List<IndexObservation> mergeIndexObservations(
            List<IndexObservation> existing, List<IndexObservation> incoming) {
        Map<java.time.LocalDate, IndexObservation> byDate = new TreeMap<>();
        for (IndexObservation observation : existing) {
            byDate.put(observation.date(), observation);
        }
        for (IndexObservation observation : incoming) {
            byDate.put(observation.date(), observation);
        }
        return List.copyOf(byDate.values());
    }

    private static boolean writeIfChanged(Path path, byte[] content) throws IOException {
        if (Files.exists(path) && java.util.Arrays.equals(Files.readAllBytes(path), content)) {
            return false;
        }

        Path parent = path.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, content);
            try {
                Files.move(temporary, path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return true;
    }

    private static boolean removeStaleFiles(Path directory, Set<String> currentFiles) throws IOException {
        boolean changed = false;
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(value -> value.getFileName().toString().endsWith(".json"))
                    .toList()) {
                if (!currentFiles.contains(path.getFileName().toString())) {
                    changed |= Files.deleteIfExists(path);
                }
            }
        }
        return changed;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record CityFile(
            CityRef city, String valueUnit, String source, List<QuarterObservation> observations) {
    }

    private record BenchmarkFile(
            String id, String name, String valueUnit, String source, List<IndexObservation> observations) {
    }

    private record CityManifest(
            String stateId, String stateName, String cityId, String cityName, String file, String sha256) {
    }

    private record BenchmarkManifest(
            String id, String name, String valueUnit, String firstDate, String lastDate,
            int observationCount, String file, String sha256) {
    }

    private record ManifestContent(
            int schemaVersion, String cityPriceUnit, List<CityManifest> cities,
            List<BenchmarkManifest> benchmarks, List<String> sources) {
    }

    private record Manifest(
            int schemaVersion, String cityPriceUnit, List<CityManifest> cities,
            List<BenchmarkManifest> benchmarks, List<String> sources, String generatedAt) {
    }
}
