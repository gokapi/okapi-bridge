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

/**
 * Manages schema versioning across Okapi releases.
 * 
 * Schema versions are incremented only when the schema content changes.
 * This allows tracking which Okapi versions are compatible with each schema version.
 * 
 * Usage:
 *   # Generate schemas for a specific Okapi version and update version tracking
 *   mvn exec:java -Dexec.mainClass=com.gokapi.bridge.schema.SchemaVersioner \
 *       -Dexec.args="1.47.0 schemas"
 * 
 * The versioner maintains a schema-versions.json file that tracks:
 * - Each filter's schema versions
 * - Which Okapi versions each schema version supports
 * - Content hashes for change detection
 */
public class SchemaVersioner {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String VERSIONS_FILE = "schema-versions.json";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: SchemaVersioner <okapi-version> <schemas-dir> [versions-file]");
            System.err.println("Example: SchemaVersioner 1.47.0 okapi-releases/1.47.0/schemas schema-versions.json");
            System.exit(1);
        }

        String okapiVersion = args[0];
        String schemasDir = args[1];
        String versionsFile = args.length > 2 ? args[2] : VERSIONS_FILE;

        try {
            SchemaVersioner versioner = new SchemaVersioner();
            versioner.processSchemas(okapiVersion, schemasDir, versionsFile);
        } catch (Exception e) {
            System.err.println("Schema versioning failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Process schemas for a given Okapi version.
     * 
     * 1. Load existing version history from versionsFile
     * 2. Compare each schema's content hash
     * 3. Increment version if content changed, otherwise add Okapi version to existing
     * 4. Update schemas with correct version and compatibility info
     * 5. Save updated version history
     */
    public void processSchemas(String okapiVersion, String schemasDir, String versionsFile) throws Exception {
        Path schemasDirPath = Path.of(schemasDir);
        Path versionsFilePath = Path.of(versionsFile);

        // Load existing version history (or create new)
        JsonObject versionHistory = loadVersionHistory(versionsFilePath);

        // Get list of schema files
        File[] schemaFiles = schemasDirPath.toFile().listFiles(
                (dir, name) -> name.endsWith(".schema.json"));

        if (schemaFiles == null || schemaFiles.length == 0) {
            System.err.println("No schema files found in " + schemasDir);
            return;
        }

        System.out.println("Processing " + schemaFiles.length + " schemas for Okapi " + okapiVersion + "...\n");

        int newVersions = 0;
        int updatedVersions = 0;
        int unchangedVersions = 0;

        for (File schemaFile : schemaFiles) {
            String filterId = schemaFile.getName().replace(".schema.json", "");
            
            // Read current schema
            JsonObject schema = readJsonFile(schemaFile);
            
            // Calculate content hash (excluding version metadata)
            String contentHash = calculateSchemaHash(schema);
            
            // Get or create filter version history
            JsonObject filterHistory = getOrCreateFilterHistory(versionHistory, filterId);
            JsonArray versions = filterHistory.getAsJsonArray("versions");
            
            // Find if this hash already exists
            JsonObject matchingVersion = findVersionByHash(versions, contentHash);
            
            if (matchingVersion != null) {
                // Same content - add Okapi version to existing schema version
                JsonArray okapiVersions = matchingVersion.getAsJsonArray("okapiVersions");
                if (!containsString(okapiVersions, okapiVersion)) {
                    okapiVersions.add(okapiVersion);
                    unchangedVersions++;
                    System.out.println("= " + filterId + " (v" + matchingVersion.get("schemaVersion").getAsString() + 
                            " now supports Okapi " + okapiVersion + ")");
                }
                
                // Update schema file with version info
                updateSchemaVersion(schema, matchingVersion, okapiVersion);
            } else {
                // New content - create new schema version
                int newSchemaVersion = versions.size() + 1;
                
                JsonObject newVersion = new JsonObject();
                newVersion.addProperty("schemaVersion", newSchemaVersion);
                newVersion.addProperty("contentHash", contentHash);
                newVersion.addProperty("introducedInOkapi", okapiVersion);
                
                JsonArray okapiVersions = new JsonArray();
                okapiVersions.add(okapiVersion);
                newVersion.add("okapiVersions", okapiVersions);
                
                versions.add(newVersion);
                
                if (newSchemaVersion == 1) {
                    newVersions++;
                    System.out.println("+ " + filterId + " v1 (new schema)");
                } else {
                    updatedVersions++;
                    System.out.println("↑ " + filterId + " v" + (newSchemaVersion - 1) + " → v" + newSchemaVersion + 
                            " (changed in Okapi " + okapiVersion + ")");
                }
                
                // Update schema file with version info
                updateSchemaVersion(schema, newVersion, okapiVersion);
            }
            
            // Write updated schema back
            writeJsonFile(schemaFile, schema);
        }

        // Save version history
        saveVersionHistory(versionsFilePath, versionHistory);

        System.out.println("\nProcessing complete:");
        System.out.println("  New schemas: " + newVersions);
        System.out.println("  Updated schemas: " + updatedVersions);
        System.out.println("  Unchanged schemas: " + unchangedVersions);
    }

    /**
     * Calculate a content hash of the schema, excluding version metadata.
     * This allows detecting actual content changes vs. version number changes.
     */
    private String calculateSchemaHash(JsonObject schema) throws Exception {
        // Create a copy without version-related fields
        JsonObject normalized = schema.deepCopy();
        normalized.remove("$version");
        normalized.remove("x-schemaVersion");
        normalized.remove("x-okapiVersions");
        normalized.remove("x-introducedInOkapi");
        
        // Sort keys for consistent hashing
        String normalizedJson = GSON.toJson(sortJsonObject(normalized));
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(normalizedJson.getBytes(StandardCharsets.UTF_8));
        
        // Return first 12 chars of hex (enough for uniqueness)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%02x", hash[i]));
        }
        return sb.toString();
    }

    /**
     * Recursively sort JSON object keys for consistent hashing.
     */
    private JsonElement sortJsonObject(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject sorted = new JsonObject();
            
            List<String> keys = new ArrayList<>(obj.keySet());
            Collections.sort(keys);
            
            for (String key : keys) {
                sorted.add(key, sortJsonObject(obj.get(key)));
            }
            return sorted;
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            JsonArray sorted = new JsonArray();
            for (JsonElement e : arr) {
                sorted.add(sortJsonObject(e));
            }
            return sorted;
        }
        return element;
    }

    private JsonObject loadVersionHistory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        
        // Create new version history
        JsonObject history = new JsonObject();
        history.addProperty("$schema", "https://gokapi.dev/schemas/schema-versions.json");
        history.add("filters", new JsonObject());
        return history;
    }

    private void saveVersionHistory(Path path, JsonObject history) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(history, writer);
        }
    }

    private JsonObject getOrCreateFilterHistory(JsonObject history, String filterId) {
        JsonObject filters = history.getAsJsonObject("filters");
        
        if (!filters.has(filterId)) {
            JsonObject filterHistory = new JsonObject();
            filterHistory.add("versions", new JsonArray());
            filters.add(filterId, filterHistory);
        }
        
        return filters.getAsJsonObject(filterId);
    }

    private JsonObject findVersionByHash(JsonArray versions, String hash) {
        for (JsonElement e : versions) {
            JsonObject v = e.getAsJsonObject();
            if (hash.equals(v.get("contentHash").getAsString())) {
                return v;
            }
        }
        return null;
    }

    private boolean containsString(JsonArray arr, String value) {
        for (JsonElement e : arr) {
            if (value.equals(e.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private void updateSchemaVersion(JsonObject schema, JsonObject versionInfo, String currentOkapiVersion) {
        int schemaVersion = versionInfo.get("schemaVersion").getAsInt();
        JsonArray okapiVersions = versionInfo.getAsJsonArray("okapiVersions");
        String introducedIn = versionInfo.get("introducedInOkapi").getAsString();
        
        // Update $version to include schema version
        schema.addProperty("$version", schemaVersion + ".0.0");
        
        // Add extension fields
        schema.addProperty("x-schemaVersion", schemaVersion);
        schema.add("x-okapiVersions", okapiVersions.deepCopy());
        schema.addProperty("x-introducedInOkapi", introducedIn);
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
}
