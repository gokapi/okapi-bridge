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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates JSON Schema files for all registered Okapi filters.
 * 
 * This tool introspects filter Parameters classes at build time using reflection,
 * then transforms the extracted parameter metadata into clean JSON Schema (draft-07)
 * format suitable for validation and UI generation.
 * 
 * Run via: mvn exec:java -Dexec.mainClass=com.gokapi.bridge.schema.SchemaGenerator
 */
public class SchemaGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String SCHEMA_VERSION = "1.0.0";
    private static final String OUTPUT_DIR = "schemas";

    private final ParameterIntrospector introspector;
    private final SchemaTransformer transformer;
    private File overridesDir;

    public SchemaGenerator() {
        this.introspector = new ParameterIntrospector();
        this.transformer = new SchemaTransformer();
    }

    public static void main(String[] args) {
        SchemaGenerator generator = new SchemaGenerator();
        
        String outputDir = OUTPUT_DIR;
        if (args.length > 0) {
            outputDir = args[0];
        }

        try {
            generator.generateAll(outputDir);
        } catch (Exception e) {
            System.err.println("Schema generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Generate schemas for all registered filters.
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
        List<FilterInfo> availableFilters = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        System.out.println("Checking filter availability...\n");

        // First pass: check which filters are available on the classpath
        for (FilterInfo info : filters) {
            if (isFilterAvailable(info.getFilterClass())) {
                availableFilters.add(info);
            } else {
                System.out.println("⊘ " + info.getName() + " → not available in this Okapi version");
                skippedCount++;
            }
        }

        System.out.println("\nGenerating schemas for " + availableFilters.size() + " available filters...\n");

        for (FilterInfo info : availableFilters) {
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

        System.out.println("\nGeneration complete: " + successCount + " schemas, " + failCount + " failures, " + skippedCount + " skipped");

        // Generate meta.yaml with filter metadata (only for available filters)
        generateMetaFile(dir, availableFilters);
    }

    /**
     * Check if a filter class is available on the classpath.
     */
    private boolean isFilterAvailable(String filterClass) {
        try {
            Class.forName(filterClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Generate a JSON Schema for a single filter.
     * Always returns a schema, even for filters with no parameters.
     */
    public JsonObject generateSchema(FilterInfo info) {
        String filterClass = info.getFilterClass();
        String filterId = "okf_" + info.getName();

        // Build base schema
        JsonObject schema = new JsonObject();
        schema.addProperty("$schema", "http://json-schema.org/draft-07/schema#");
        schema.addProperty("$id", "https://gokapi.dev/schemas/filters/" + filterId + ".schema.json");
        schema.addProperty("$version", SCHEMA_VERSION);
        schema.addProperty("title", info.getDisplayName() + " Filter");
        schema.addProperty("description", "Configuration for the Okapi " + info.getDisplayName() + " Filter");
        schema.addProperty("type", "object");

        // Add filter metadata extension
        JsonObject filterMeta = new JsonObject();
        filterMeta.addProperty("id", filterId);
        filterMeta.addProperty("class", filterClass);
        filterMeta.add("extensions", GSON.toJsonTree(info.getExtensions()));
        filterMeta.add("mimeTypes", GSON.toJsonTree(info.getMimeTypes()));
        schema.add("x-filter", filterMeta);

        // Introspect the filter's Parameters class
        Map<String, ParameterIntrospector.ParamInfo> params = introspector.introspect(filterClass);

        // Build properties (may be empty)
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

        // Try to load and merge overrides
        JsonObject overrides = loadOverrides(filterId);
        if (overrides != null) {
            transformer.mergeEditorHints(schema, overrides);
        }

        schema.addProperty("additionalProperties", false);

        return schema;
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
     * Generate meta.yaml with filter metadata.
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
