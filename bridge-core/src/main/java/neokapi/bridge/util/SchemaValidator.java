package neokapi.bridge.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates filter configuration against a JSON Schema.
 * <p>
 * This is a lightweight validator that checks:
 * - Property types (string, boolean, integer, number, object, array)
 * - Enum constraints
 * - Required properties
 * - Unknown properties (warning, not error)
 * <p>
 * It resolves local $ref references (to $defs) within the schema.
 */
public class SchemaValidator {

    private final JsonObject schema;

    public SchemaValidator(JsonObject schema) {
        this.schema = schema;
    }

    /**
     * Validate a configuration object against the schema.
     *
     * @param config the configuration to validate
     * @return validation result with errors and warnings
     */
    public ValidationResult validate(JsonObject config) {
        ValidationResult result = new ValidationResult();
        JsonObject properties = schema.getAsJsonObject("properties");
        if (properties == null) {
            return result;
        }
        validateObject(config, properties, schema, "", result);
        return result;
    }

    private void validateObject(JsonObject obj, JsonObject properties,
                                 JsonObject schemaContext, String path,
                                 ValidationResult result) {
        // Check for unknown properties
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            String propPath = path.isEmpty() ? key : path + "." + key;
            if (!properties.has(key)) {
                result.addWarning(propPath, "Unknown property");
                continue;
            }
            JsonObject propSchema = resolveRef(properties.getAsJsonObject(key), schemaContext);
            if (propSchema != null) {
                validateValue(entry.getValue(), propSchema, schemaContext, propPath, result);
            }
        }

        // Check required properties
        if (schemaContext.has("required")) {
            JsonArray required = schemaContext.getAsJsonArray("required");
            if (required != null) {
                for (JsonElement req : required) {
                    String reqName = req.getAsString();
                    String reqPath = path.isEmpty() ? reqName : path + "." + reqName;
                    if (!obj.has(reqName)) {
                        result.addError(reqPath, "Required property missing");
                    }
                }
            }
        }
    }

    private void validateValue(JsonElement value, JsonObject propSchema,
                                JsonObject schemaContext, String path,
                                ValidationResult result) {
        String expectedType = propSchema.has("type")
                ? propSchema.get("type").getAsString() : null;

        if (expectedType == null) {
            return; // No type constraint
        }

        switch (expectedType) {
            case "string":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    result.addError(path, "Expected string, got " + jsonType(value));
                    return;
                }
                validateEnum(value.getAsString(), propSchema, path, result);
                break;

            case "boolean":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                    // Allow string "true"/"false" since gRPC map<string,string> encodes booleans as strings
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        String s = value.getAsString();
                        if (!"true".equals(s) && !"false".equals(s)) {
                            result.addError(path, "Expected boolean, got string \"" + s + "\"");
                        }
                    } else {
                        result.addError(path, "Expected boolean, got " + jsonType(value));
                    }
                }
                break;

            case "integer":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        try {
                            Integer.parseInt(value.getAsString());
                        } catch (NumberFormatException e) {
                            result.addError(path, "Expected integer, got string \"" + value.getAsString() + "\"");
                        }
                    } else {
                        result.addError(path, "Expected integer, got " + jsonType(value));
                    }
                }
                break;

            case "number":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        try {
                            Double.parseDouble(value.getAsString());
                        } catch (NumberFormatException e) {
                            result.addError(path, "Expected number, got string \"" + value.getAsString() + "\"");
                        }
                    } else {
                        result.addError(path, "Expected number, got " + jsonType(value));
                    }
                }
                break;

            case "object":
                if (!value.isJsonObject()) {
                    result.addError(path, "Expected object, got " + jsonType(value));
                    return;
                }
                if (propSchema.has("properties")) {
                    validateObject(value.getAsJsonObject(),
                            propSchema.getAsJsonObject("properties"),
                            propSchema, path, result);
                }
                break;

            case "array":
                if (!value.isJsonArray()) {
                    result.addError(path, "Expected array, got " + jsonType(value));
                }
                break;

            default:
                break;
        }
    }

    private void validateEnum(String value, JsonObject propSchema,
                               String path, ValidationResult result) {
        if (!propSchema.has("enum")) {
            return;
        }
        JsonArray enumValues = propSchema.getAsJsonArray("enum");
        for (JsonElement ev : enumValues) {
            if (ev.isJsonPrimitive() && ev.getAsString().equals(value)) {
                return;
            }
        }
        result.addError(path, "Value \"" + value + "\" not in enum " + enumValues);
    }

    private JsonObject resolveRef(JsonObject propSchema, JsonObject schemaContext) {
        if (propSchema == null) {
            return null;
        }
        if (!propSchema.has("$ref")) {
            return propSchema;
        }
        String ref = propSchema.get("$ref").getAsString();
        if (ref.startsWith("#/$defs/")) {
            String defName = ref.substring("#/$defs/".length());
            JsonObject defs = schemaContext.getAsJsonObject("$defs");
            if (defs == null) {
                defs = schema.getAsJsonObject("$defs");
            }
            if (defs != null && defs.has(defName)) {
                return defs.getAsJsonObject(defName);
            }
        }
        return propSchema;
    }

    private static String jsonType(JsonElement value) {
        if (value == null || value.isJsonNull()) return "null";
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) return "boolean";
            if (value.getAsJsonPrimitive().isNumber()) return "number";
            if (value.getAsJsonPrimitive().isString()) return "string";
        }
        if (value.isJsonObject()) return "object";
        if (value.isJsonArray()) return "array";
        return "unknown";
    }

    /**
     * Result of schema validation.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        void addError(String path, String message) {
            errors.add(path + ": " + message);
        }

        void addWarning(String path, String message) {
            warnings.add(path + ": " + message);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Errors: ");
                sb.append(String.join("; ", errors));
            }
            if (!warnings.isEmpty()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append("Warnings: ");
                sb.append(String.join("; ", warnings));
            }
            return sb.length() > 0 ? sb.toString() : "Valid";
        }
    }
}
