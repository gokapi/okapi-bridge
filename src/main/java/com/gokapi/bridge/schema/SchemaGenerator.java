package com.gokapi.bridge.schema;

import com.gokapi.bridge.model.FilterInfo;
import com.gokapi.bridge.util.FilterRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Generates base JSON Schema files for Okapi filters.
 * 
 * This tool introspects filter Parameters classes at build time using reflection,
 * then transforms the extracted parameter metadata into clean JSON Schema (draft-07).
 * 
 * Output is base schemas only - merging with overrides is done by bash/jq scripts.
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
     * Generate base schemas for all discovered filters.
     */
    public void generateAll(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }

        List<FilterInfo> filters = FilterRegistry.listFilters();
        int successCount = 0;
        int failCount = 0;

        System.out.println("Generating base schemas for " + filters.size() + " filters...\n");

        for (FilterInfo info : filters) {
            try {
                JsonObject schema = generateBaseSchema(info);
                String filterId = "okf_" + info.getName();
                String filename = filterId + ".schema.json";
                File outputFile = new File(dir, filename);
                
                try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                    GSON.toJson(schema, writer);
                }
                
                int paramCount = schema.getAsJsonObject("properties").size();
                System.out.println("✓ " + filterId + " (" + paramCount + " params)");
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
     * Generate a base schema for a single filter (no override merging).
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
        
        // Include filter configurations (presets/variants)
        if (info.getConfigurations() != null && !info.getConfigurations().isEmpty()) {
            filterMeta.add("configurations", GSON.toJsonTree(info.getConfigurations()));
        }
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
            
            // Include filter configurations
            if (info.getConfigurations() != null && !info.getConfigurations().isEmpty()) {
                filterMeta.add("configurations", GSON.toJsonTree(info.getConfigurations()));
            }
            filtersObj.add(filterId, filterMeta);
        }
        meta.add("filters", filtersObj);

        try (FileWriter writer = new FileWriter(metaFile, StandardCharsets.UTF_8)) {
            GSON.toJson(meta, writer);
        }
        
        System.out.println("Generated meta.json");
    }
}
