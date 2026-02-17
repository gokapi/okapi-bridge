package com.gokapi.bridge.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages centralized schema generation and versioning.
 * 
 * This class handles the full workflow:
 * 1. Generate base schemas for each Okapi version
 * 2. Merge with centralized overrides to create composites
 * 3. Track versions based on composite content hash
 * 4. Deduplicate identical schemas across versions
 * 
 * Usage:
 *   mvn exec:java -Dexec.mainClass=com.gokapi.bridge.schema.CentralizedSchemaManager \
 *       -Dexec.args="regenerate-all"
 */
public class CentralizedSchemaManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Gson CANONICAL_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private static final String SCHEMAS_DIR = "schemas";
    private static final String OVERRIDES_DIR = "overrides";
    private static final String OKAPI_RELEASES_DIR = "okapi-releases";
    private static final String VERSIONS_FILE = "schema-versions.json";

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        CentralizedSchemaManager manager = new CentralizedSchemaManager();

        try {
            switch (command) {
                case "regenerate-all":
                    manager.regenerateAll();
                    break;
                case "regenerate-composites":
                    manager.regenerateComposites();
                    break;
                case "add-version":
                    if (args.length < 2) {
                        System.err.println("Usage: add-version <okapi-version>");
                        System.exit(1);
                    }
                    manager.addVersion(args[1]);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: CentralizedSchemaManager <command>");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  regenerate-all        Regenerate all base and composite schemas");
        System.err.println("  regenerate-composites Regenerate composites from existing bases");
        System.err.println("  add-version <ver>     Add a new Okapi version");
    }

    /**
     * Regenerate all schemas from scratch.
     * Reads base schemas from each okapi-releases/VERSION/schemas/ directory,
     * merges with overrides, and writes to centralized location.
     */
    public void regenerateAll() throws Exception {
        Path schemasPath = Path.of(SCHEMAS_DIR);
        Path basePath = schemasPath.resolve("base");
        Path compositePath = schemasPath.resolve("composite");
        Path overridesPath = Path.of(OVERRIDES_DIR);

        // Create directories
        Files.createDirectories(basePath);
        Files.createDirectories(compositePath);

        // Get all Okapi versions
        List<String> versions = getOkapiVersions();
        System.out.println("Processing " + versions.size() + " Okapi versions...\n");

        // Track all schema data for version file
        Map<String, FilterVersionData> filterData = new TreeMap<>();

        for (String okapiVersion : versions) {
            System.out.println("=== Okapi " + okapiVersion + " ===");
            Path versionSchemas = Path.of(OKAPI_RELEASES_DIR, okapiVersion, "schemas");

            if (!Files.exists(versionSchemas)) {
                System.out.println("  No schemas found, skipping");
                continue;
            }

            // Process each schema in this version
            File[] schemaFiles = versionSchemas.toFile().listFiles(
                    (dir, name) -> name.endsWith(".schema.json") && !name.equals("meta.json"));

            if (schemaFiles == null) continue;

            for (File schemaFile : schemaFiles) {
                String filterId = schemaFile.getName().replace(".schema.json", "");
                processFilterSchema(filterId, schemaFile, okapiVersion, basePath, 
                        compositePath, overridesPath, filterData);
            }
            System.out.println();
        }

        // Write schema-versions.json
        writeVersionsFile(filterData);
        
        // Clean up per-version schemas and overrides
        cleanupPerVersionFiles(versions);

        System.out.println("\n=== Summary ===");
        System.out.println("Total filters: " + filterData.size());
        int totalComposites = filterData.values().stream()
                .mapToInt(f -> f.compositeVersions.size()).sum();
        System.out.println("Total composite versions: " + totalComposites);
    }

    /**
     * Regenerate only composite schemas from existing base schemas.
     * Useful when overrides change but base schemas haven't.
     */
    public void regenerateComposites() throws Exception {
        Path basePath = Path.of(SCHEMAS_DIR, "base");
        Path compositePath = Path.of(SCHEMAS_DIR, "composite");
        Path overridesPath = Path.of(OVERRIDES_DIR);

        if (!Files.exists(basePath)) {
            throw new IOException("Base schemas not found. Run 'regenerate-all' first.");
        }

        // Load existing versions file
        JsonObject existingVersions = loadVersionsFile();
        Map<String, FilterVersionData> filterData = parseVersionsFile(existingVersions);

        // Clear composite directory
        if (Files.exists(compositePath)) {
            Files.walk(compositePath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(compositePath);

        System.out.println("Regenerating composite schemas...\n");

        // For each filter, regenerate composites
        for (Map.Entry<String, FilterVersionData> entry : filterData.entrySet()) {
            String filterId = entry.getKey();
            FilterVersionData data = entry.getValue();

            // Load override if exists
            JsonObject override = loadOverride(overridesPath, filterId);
            String overrideHash = override != null ? computeHash(override) : null;

            // Regenerate each composite version
            for (CompositeVersion cv : data.compositeVersions) {
                // Load base schema
                Path baseFile = basePath.resolve(filterId + ".base." + cv.baseHash + ".json");
                if (!Files.exists(baseFile)) {
                    System.err.println("Warning: Base file not found: " + baseFile);
                    continue;
                }

                JsonObject base = readJsonFile(baseFile.toFile());
                JsonObject composite = mergeSchemas(base, override);
                String newCompositeHash = computeHash(composite);

                // Check if composite changed
                if (!newCompositeHash.equals(cv.compositeHash)) {
                    // Composite changed due to override change
                    cv.compositeHash = newCompositeHash;
                    cv.overrideHash = overrideHash;
                    System.out.println("↑ " + filterId + " v" + cv.version + " composite updated");
                }

                // Write composite
                Path compositeFile = compositePath.resolve(
                        filterId + ".v" + cv.version + ".schema.json");
                addVersionMetadata(composite, cv);
                writeJsonFile(compositeFile.toFile(), composite);
            }
        }

        writeVersionsFile(filterData);
        System.out.println("\nComposite regeneration complete.");
    }

    /**
     * Add a new Okapi version.
     */
    public void addVersion(String okapiVersion) throws Exception {
        System.out.println("Adding Okapi " + okapiVersion + "...");
        // This would typically be called after make add-release
        // For now, just trigger a full regeneration
        regenerateAll();
    }

    private void processFilterSchema(String filterId, File schemaFile, String okapiVersion,
                                     Path basePath, Path compositePath, Path overridesPath,
                                     Map<String, FilterVersionData> filterData) throws Exception {

        // Read schema (this is the merged schema from legacy generation)
        JsonObject schema = readJsonFile(schemaFile);
        
        // Remove override-merged fields to get base
        JsonObject base = extractBaseSchema(schema);
        String baseHash = computeHash(base);

        // Load override
        JsonObject override = loadOverride(overridesPath, filterId);
        String overrideHash = override != null ? computeHash(override) : null;

        // Generate composite
        JsonObject composite = mergeSchemas(base, override);
        String compositeHash = computeHash(composite);

        // Get or create filter data
        FilterVersionData data = filterData.computeIfAbsent(filterId, k -> new FilterVersionData());

        // Check if this composite already exists
        CompositeVersion existing = data.findByCompositeHash(compositeHash);

        if (existing != null) {
            // Same composite - add okapi version
            if (!existing.okapiVersions.contains(okapiVersion)) {
                existing.okapiVersions.add(okapiVersion);
            }
            System.out.println("  = " + filterId + " v" + existing.version + " (unchanged)");
        } else {
            // New composite version
            int newVersion = data.getNextVersion();
            CompositeVersion cv = new CompositeVersion();
            cv.version = newVersion;
            cv.baseHash = baseHash;
            cv.overrideHash = overrideHash;
            cv.compositeHash = compositeHash;
            cv.introducedInOkapi = okapiVersion;
            cv.okapiVersions.add(okapiVersion);
            data.compositeVersions.add(cv);

            // Write base schema (deduplicated by hash)
            Path baseFile = basePath.resolve(filterId + ".base." + baseHash + ".json");
            if (!Files.exists(baseFile)) {
                writeJsonFile(baseFile.toFile(), base);
            }

            // Write composite schema
            Path compositeFile = compositePath.resolve(filterId + ".v" + newVersion + ".schema.json");
            addVersionMetadata(composite, cv);
            writeJsonFile(compositeFile.toFile(), composite);

            if (newVersion == 1) {
                System.out.println("  + " + filterId + " v1 (new)");
            } else {
                System.out.println("  ↑ " + filterId + " v" + newVersion + " (changed)");
            }
        }
    }

    /**
     * Extract base schema by removing override-specific fields.
     */
    private JsonObject extractBaseSchema(JsonObject schema) {
        JsonObject base = schema.deepCopy();
        // Remove x-groups (from overrides)
        base.remove("x-groups");
        // Remove version metadata we'll re-add
        base.remove("x-schemaVersion");
        base.remove("x-okapiVersions");
        base.remove("x-introducedInOkapi");
        
        // Remove x-widget, x-placeholder, x-presets from properties
        if (base.has("properties")) {
            JsonObject props = base.getAsJsonObject("properties");
            for (String key : props.keySet()) {
                JsonObject prop = props.getAsJsonObject(key);
                if (prop != null) {
                    prop.remove("x-widget");
                    prop.remove("x-placeholder");
                    prop.remove("x-presets");
                    prop.remove("x-order");
                    prop.remove("x-showIf");
                }
            }
        }
        return base;
    }

    /**
     * Merge base schema with override.
     */
    private JsonObject mergeSchemas(JsonObject base, JsonObject override) {
        JsonObject merged = base.deepCopy();
        if (override != null) {
            new SchemaTransformer().mergeEditorHints(merged, override);
        }
        return merged;
    }

    /**
     * Add version metadata to composite schema.
     */
    private void addVersionMetadata(JsonObject schema, CompositeVersion cv) {
        schema.addProperty("$version", cv.version + ".0.0");
        schema.addProperty("x-schemaVersion", cv.version);
        schema.addProperty("x-introducedInOkapi", cv.introducedInOkapi);
        
        JsonArray okapiVersions = new JsonArray();
        for (String v : cv.okapiVersions) {
            okapiVersions.add(v);
        }
        schema.add("x-okapiVersions", okapiVersions);
        
        // Add hash metadata
        schema.addProperty("x-baseHash", cv.baseHash);
        if (cv.overrideHash != null) {
            schema.addProperty("x-overrideHash", cv.overrideHash);
        }
        schema.addProperty("x-compositeHash", cv.compositeHash);
    }

    private JsonObject loadOverride(Path overridesPath, String filterId) {
        Path overrideFile = overridesPath.resolve(filterId + ".overrides.json");
        if (!Files.exists(overrideFile)) {
            return null;
        }
        try {
            return readJsonFile(overrideFile.toFile());
        } catch (Exception e) {
            System.err.println("Warning: Failed to load override for " + filterId);
            return null;
        }
    }

    private String computeHash(JsonObject json) {
        try {
            String canonical = toCanonicalJson(json);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private String toCanonicalJson(JsonObject json) {
        return CANONICAL_GSON.toJson(sortJsonObject(json));
    }

    private JsonObject sortJsonObject(JsonObject obj) {
        TreeMap<String, JsonElement> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                sorted.put(entry.getKey(), sortJsonObject(value.getAsJsonObject()));
            } else {
                sorted.put(entry.getKey(), value);
            }
        }
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : sorted.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private List<String> getOkapiVersions() throws IOException {
        Path releasesDir = Path.of(OKAPI_RELEASES_DIR);
        if (!Files.exists(releasesDir)) {
            return Collections.emptyList();
        }
        return Files.list(releasesDir)
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .filter(n -> n.matches("\\d.*"))
                .sorted(this::compareVersions)
                .collect(Collectors.toList());
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;
            if (p1 != p2) return p1 - p2;
        }
        return 0;
    }

    private void writeVersionsFile(Map<String, FilterVersionData> filterData) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("$schema", "https://gokapi.dev/schemas/schema-versions.json");
        root.addProperty("generatedAt", java.time.Instant.now().toString());

        JsonObject filters = new JsonObject();
        for (Map.Entry<String, FilterVersionData> entry : filterData.entrySet()) {
            String filterId = entry.getKey();
            FilterVersionData data = entry.getValue();

            JsonObject filterObj = new JsonObject();
            JsonArray versionsArray = new JsonArray();

            for (CompositeVersion cv : data.compositeVersions) {
                JsonObject versionObj = new JsonObject();
                versionObj.addProperty("version", cv.version);
                versionObj.addProperty("baseHash", cv.baseHash);
                if (cv.overrideHash != null) {
                    versionObj.addProperty("overrideHash", cv.overrideHash);
                }
                versionObj.addProperty("compositeHash", cv.compositeHash);
                versionObj.addProperty("introducedInOkapi", cv.introducedInOkapi);

                JsonArray okapiVersions = new JsonArray();
                for (String v : cv.okapiVersions) {
                    okapiVersions.add(v);
                }
                versionObj.add("okapiVersions", okapiVersions);
                versionsArray.add(versionObj);
            }

            filterObj.add("versions", versionsArray);
            filters.add(filterId, filterObj);
        }

        root.add("filters", filters);
        writeJsonFile(new File(VERSIONS_FILE), root);
        System.out.println("\nUpdated " + VERSIONS_FILE);
    }

    private JsonObject loadVersionsFile() throws IOException {
        File file = new File(VERSIONS_FILE);
        if (!file.exists()) {
            JsonObject root = new JsonObject();
            root.add("filters", new JsonObject());
            return root;
        }
        return readJsonFile(file);
    }

    private Map<String, FilterVersionData> parseVersionsFile(JsonObject root) {
        Map<String, FilterVersionData> result = new TreeMap<>();
        JsonObject filters = root.getAsJsonObject("filters");
        if (filters == null) return result;

        for (String filterId : filters.keySet()) {
            FilterVersionData data = new FilterVersionData();
            JsonObject filterObj = filters.getAsJsonObject(filterId);
            JsonArray versions = filterObj.getAsJsonArray("versions");

            for (JsonElement e : versions) {
                JsonObject v = e.getAsJsonObject();
                CompositeVersion cv = new CompositeVersion();
                cv.version = v.get("version").getAsInt();
                cv.baseHash = v.get("baseHash").getAsString();
                cv.overrideHash = v.has("overrideHash") ? v.get("overrideHash").getAsString() : null;
                cv.compositeHash = v.get("compositeHash").getAsString();
                cv.introducedInOkapi = v.get("introducedInOkapi").getAsString();
                for (JsonElement ov : v.getAsJsonArray("okapiVersions")) {
                    cv.okapiVersions.add(ov.getAsString());
                }
                data.compositeVersions.add(cv);
            }
            result.put(filterId, data);
        }
        return result;
    }

    private void cleanupPerVersionFiles(List<String> versions) throws IOException {
        System.out.println("\nCleaning up per-version files...");
        for (String version : versions) {
            Path versionDir = Path.of(OKAPI_RELEASES_DIR, version);
            
            // Remove schemas directory
            Path schemasDir = versionDir.resolve("schemas");
            if (Files.exists(schemasDir)) {
                Files.walk(schemasDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("  Removed " + schemasDir);
            }
            
            // Remove overrides directory
            Path overridesDir = versionDir.resolve("overrides");
            if (Files.exists(overridesDir)) {
                Files.walk(overridesDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("  Removed " + overridesDir);
            }
        }
    }

    private JsonObject readJsonFile(File file) throws IOException {
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private void writeJsonFile(File file, JsonObject json) throws IOException {
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    // Inner classes for data structures
    private static class FilterVersionData {
        List<CompositeVersion> compositeVersions = new ArrayList<>();

        CompositeVersion findByCompositeHash(String hash) {
            return compositeVersions.stream()
                    .filter(v -> v.compositeHash.equals(hash))
                    .findFirst()
                    .orElse(null);
        }

        int getNextVersion() {
            return compositeVersions.stream()
                    .mapToInt(v -> v.version)
                    .max()
                    .orElse(0) + 1;
        }
    }

    private static class CompositeVersion {
        int version;
        String baseHash;
        String overrideHash;
        String compositeHash;
        String introducedInOkapi;
        List<String> okapiVersions = new ArrayList<>();
    }
}
