package com.gokapi.bridge.schema;

import com.gokapi.bridge.model.FilterInfo;
import com.gokapi.bridge.util.FilterRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates JSON Schema files for all registered Okapi filters.
 * 
 * Supports two modes:
 * 1. Legacy mode (for per-version generation): outputs merged schemas to a single directory
 * 2. Centralized mode (--centralized): outputs base schemas and composites to separate directories
 * 
 * Run via: mvn exec:java -Dexec.mainClass=com.gokapi.bridge.schema.SchemaGenerator
 */
public class SchemaGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    // Canonical JSON for stable hashing (sorted keys, no pretty printing)
    private static final Gson CANONICAL_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private static final String SCHEMA_VERSION = "1.0.0";
    private static final String OUTPUT_DIR = "schemas";

    private final ParameterIntrospector introspector;
    private final SchemaTransformer transformer;
    private File overridesDir;
    private boolean centralizedMode = false;

    public SchemaGenerator() {
        this.introspector = new ParameterIntrospector();
        this.transformer = new SchemaTransformer();
    }

    public static void main(String[] args) {
        SchemaGenerator generator = new SchemaGenerator();
        
        String outputDir = OUTPUT_DIR;
        boolean centralized = false;
        String okapiVersion = null;
        
        for (int i = 0; i < args.length; i++) {
            if ("--centralized".equals(args[i])) {
                centralized = true;
            } else if ("--okapi-version".equals(args[i]) && i + 1 < args.length) {
                okapiVersion = args[++i];
            } else if (!args[i].startsWith("-")) {
                outputDir = args[i];
            }
        }

        try {
            if (centralized) {
                generator.generateCentralized(outputDir, okapiVersion);
            } else {
                generator.generateAll(outputDir);
            }
        } catch (Exception e) {
            System.err.println("Schema generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Generate schemas in centralized mode.
     * Base schemas go to {outputDir}/base/, composites go to {outputDir}/composite/
     */
    public void generateCentralized(String outputDir, String okapiVersion) throws IOException {
        this.centralizedMode = true;
        File baseDir = new File(outputDir, "base");
        File compositeDir = new File(outputDir, "composite");
        
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException("Failed to create base directory: " + baseDir);
        }
        if (!compositeDir.exists() && !compositeDir.mkdirs()) {
            throw new IOException("Failed to create composite directory: " + compositeDir);
        }

        // Load overrides from root overrides/ directory
        this.overridesDir = new File("overrides");
        if (overridesDir.exists()) {
            System.out.println("Loading overrides from: " + overridesDir.getAbsolutePath());
        }

        List<FilterInfo> filters = FilterRegistry.listFilters();
        int successCount = 0;
        int failCount = 0;

        System.out.println("Generating schemas for " + filters.size() + " filters (centralized mode)...\n");

        for (FilterInfo info : filters) {
            try {
                String filterId = "okf_" + info.getName();
                
                // Generate base schema (without overrides)
                JsonObject baseSchema = generateBaseSchema(info);
                String baseHash = computeHash(baseSchema);
                
                // Load override if exists
                JsonObject override = loadOverrides(filterId);
                String overrideHash = override != null ? computeHash(override) : null;
                
                // Generate composite (base + override merged)
                JsonObject composite = generateComposite(baseSchema, override);
                String compositeHash = computeHash(composite);
                
                // Save base schema with hash suffix for deduplication
                String baseFilename = filterId + ".base." + baseHash + ".json";
                File baseFile = new File(baseDir, baseFilename);
                if (!baseFile.exists()) {
                    try (FileWriter writer = new FileWriter(baseFile, StandardCharsets.UTF_8)) {
                        GSON.toJson(baseSchema, writer);
                    }
                }
                
                // Save composite with hash suffix
                String compositeFilename = filterId + "." + compositeHash + ".schema.json";
                File compositeFile = new File(compositeDir, compositeFilename);
                if (!compositeFile.exists()) {
                    try (FileWriter writer = new FileWriter(compositeFile, StandardCharsets.UTF_8)) {
                        GSON.toJson(composite, writer);
                    }
                }
                
                int paramCount = composite.getAsJsonObject("properties").size();
                System.out.println("✓ " + filterId + " (base:" + baseHash + ", override:" + 
                    (overrideHash != null ? overrideHash : "none") + ", composite:" + compositeHash + 
                    ") " + paramCount + " params");
                successCount++;
            } catch (Exception e) {
                System.err.println("✗ " + info.getName() + " → " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\nGeneration complete: " + successCount + " schemas, " + failCount + " failures");
    }

    /**
     * Generate schemas for all registered filters (legacy per-version mode).
     */
    public void generateAll(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }

        // Look for overrides in sibling directory (e.g., okapi-releases/1.47.0/overrides/)
        this.overridesDir = new File(dir.getParentFile(), "overrides");
        if (overridesDir.exists()) {
            System.out.println("Loading overrides from: " + overridesDir.getPath());
        }

        List<FilterInfo> filters = FilterRegistry.listFilters();
        int successCount = 0;
        int failCount = 0;

        System.out.println("Generating schemas for " + filters.size() + " discovered filters...\n");

        for (FilterInfo info : filters) {
            try {
                JsonObject schema = generateSchema(info);
                String filterId = "okf_" + info.getName();
                String filename = filterId + ".schema.json";
                File outputFile = new File(dir, filename);
                
                try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                    GSON.toJson(schema, writer);
                }
                
                int paramCount = schema.getAsJsonObject("properties").size();
                if (paramCount > 0) {
                    System.out.println("✓ " + filterId + " → " + filename + " (" + paramCount + " params)");
                } else {
                    System.out.println("✓ " + filterId + " → " + filename + " (no params)");
                }
                successCount++;
            } catch (Exception e) {
                System.err.println("✗ " + info.getName() + " → " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\nGeneration complete: " + successCount + " schemas, " + failCount + " failures");
        generateMetaFile(dir, filters);
    }

    /**
     * Generate a base schema without merging overrides.
     */
    public JsonObject generateBaseSchema(FilterInfo info) {
        String filterClass = info.getFilterClass();
        String filterId = "okf_" + info.getName();

        JsonObject schema = new JsonObject();
        schema.addProperty("$schema", "http://json-schema.org/draft-07/schema#");
        schema.addProperty("$id", "https://gokapi.dev/schemas/filters/" + filterId + ".schema.json");
        schema.addProperty("$version", SCHEMA_VERSION);
        schema.addProperty("title", info.getDisplayName() + " Filter");
        schema.addProperty("description", "Configuration for the Okapi " + info.getDisplayName() + " Filter");
        schema.addProperty("type", "object");

        JsonObject filterMeta = new JsonObject();
        filterMeta.addProperty("id", filterId);
        filterMeta.addProperty("class", filterClass);
        filterMeta.add("extensions", GSON.toJsonTree(info.getExtensions()));
        filterMeta.add("mimeTypes", GSON.toJsonTree(info.getMimeTypes()));
        schema.add("x-filter", filterMeta);

        Map<String, ParameterIntrospector.ParamInfo> params = introspector.introspect(filterClass);

        JsonObject properties = new JsonObject();
        if (params != null) {
            for (Map.Entry<String, ParameterIntrospector.ParamInfo> entry : params.entrySet()) {
                String paramName = entry.getKey();
                ParameterIntrospector.ParamInfo paramInfo = entry.getValue();
                
                JsonObject propSchema = transformer.transformParameter(paramName, paramInfo);
                if (propSchema != null) {
                    properties.add(paramName, propSchema);
                }
            }
        }
        schema.add("properties", properties);
        schema.addProperty("additionalProperties", false);

        return schema;
    }

    /**
     * Generate a composite schema by merging base with override.
     */
    public JsonObject generateComposite(JsonObject base, JsonObject override) {
        // Deep clone the base
        JsonObject composite = GSON.fromJson(GSON.toJson(base), JsonObject.class);
        
        if (override != null) {
            transformer.mergeEditorHints(composite, override);
        }
        
        return composite;
    }

    /**
     * Generate a JSON Schema for a single filter (legacy mode with merged overrides).
     */
    public JsonObject generateSchema(FilterInfo info) {
        JsonObject baseSchema = generateBaseSchema(info);
        String filterId = "okf_" + info.getName();
        
        JsonObject override = loadOverrides(filterId);
        return generateComposite(baseSchema, override);
    }

    /**
     * Compute a 12-character hash of a JSON object using canonical form.
     */
    public String computeHash(JsonObject json) {
        try {
            // Convert to sorted map for canonical form
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

    /**
     * Convert JSON to canonical form (sorted keys, no whitespace).
     */
    private String toCanonicalJson(JsonObject json) {
        // Parse and re-serialize with sorted keys
        return CANONICAL_GSON.toJson(sortJsonObject(json));
    }

    /**
     * Recursively sort JSON object keys.
     */
    private JsonObject sortJsonObject(JsonObject obj) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
            com.google.gson.JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                sorted.put(entry.getKey(), sortJsonObject(value.getAsJsonObject()));
            } else if (value.isJsonArray()) {
                sorted.put(entry.getKey(), value);
            } else {
                sorted.put(entry.getKey(), value);
            }
        }
        return GSON.fromJson(GSON.toJson(sorted), JsonObject.class);
    }

    /**
     * Load overrides for a filter from the overrides directory.
     */
    private JsonObject loadOverrides(String filterId) {
        if (overridesDir == null || !overridesDir.exists()) {
            return null;
        }
        File overrideFile = new File(overridesDir, filterId + ".overrides.json");
        if (!overrideFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(overrideFile, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Warning: Failed to load overrides for " + filterId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Generate meta.json with filter metadata.
     */
    private void generateMetaFile(File dir, List<FilterInfo> filters) throws IOException {
        File metaFile = new File(dir, "meta.json");
        
        JsonObject meta = new JsonObject();
        meta.addProperty("schemaVersion", SCHEMA_VERSION);
        meta.addProperty("generatedAt", java.time.Instant.now().toString());
        meta.addProperty("filterCount", filters.size());

        JsonObject filtersObj = new JsonObject();
        for (FilterInfo info : filters) {
            String filterId = "okf_" + info.getName();
            JsonObject filterMeta = new JsonObject();
            filterMeta.addProperty("class", info.getFilterClass());
            filterMeta.addProperty("displayName", info.getDisplayName());
            filterMeta.add("extensions", GSON.toJsonTree(info.getExtensions()));
            filterMeta.add("mimeTypes", GSON.toJsonTree(info.getMimeTypes()));
            filtersObj.add(filterId, filterMeta);
        }
        meta.add("filters", filtersObj);

        try (FileWriter writer = new FileWriter(metaFile, StandardCharsets.UTF_8)) {
            GSON.toJson(meta, writer);
        }
        
        System.out.println("\nGenerated meta.json");
    }
}
