package neokapi.bridge.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.sf.okapi.common.IParameters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts Okapi .fprm/native configuration into JSON parameter maps.
 * <p>
 * Okapi filters store their configuration in various formats:
 * - YAML-based (most modern filters via AbstractMarkupParameters)
 * - .fprm files (StringParameters-based: key=value with #v1 header)
 * - Custom serialized formats (some specialized filters)
 * <p>
 * This converter loads the native config into a filter's IParameters,
 * then extracts the parameter values as a flat JSON map that can be
 * used with the bridge's parameter system.
 */
public class FprmConverter {

    /**
     * Load .fprm or native config content into a filter's IParameters and
     * extract the resulting parameter values as a flat JSON object.
     *
     * @param filterParams the filter's IParameters instance (will be modified)
     * @param configContent the .fprm or native config content
     * @return flat JSON object with parameter names and values, or null on failure
     */
    public static JsonObject toJson(IParameters filterParams, String configContent) {
        if (filterParams == null || configContent == null) {
            return null;
        }

        try {
            // Load the config into the parameters
            filterParams.fromString(configContent);
        } catch (Exception e) {
            System.err.println("[bridge] FprmConverter: fromString failed: " + e.getMessage());
            return null;
        }

        return extractParameters(filterParams);
    }

    /**
     * Extract all parameter values from an IParameters instance as JSON.
     * Uses reflection to get StringParameters fields if available,
     * otherwise falls back to YAML parsing.
     *
     * @param params the configured IParameters instance
     * @return flat JSON object with parameter values
     */
    public static JsonObject extractParameters(IParameters params) {
        JsonObject result = new JsonObject();

        // Try StringParameters approach first (getBoolean, getString, getInteger)
        if (params instanceof net.sf.okapi.common.StringParameters) {
            extractStringParameters((net.sf.okapi.common.StringParameters) params, result);
            return result;
        }

        // For YAML-based parameters, parse the toString() output
        String serialized = params.toString();
        if (serialized != null && !serialized.trim().isEmpty()) {
            extractFromYaml(serialized, result);
        }

        return result;
    }

    private static void extractStringParameters(net.sf.okapi.common.StringParameters params,
                                                 JsonObject result) {
        // StringParameters stores values in an internal buffer as key=value lines.
        // We can get the serialized form and parse it.
        String serialized = params.toString();
        if (serialized == null || serialized.trim().isEmpty()) {
            return;
        }

        for (String line : serialized.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // Skip comments and #v1 header
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String rawKey = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            // StringParameters uses type suffixes: key.b (boolean), key.i (integer)
            // Strip the suffix and use it for type inference.
            String key = rawKey;
            String typeSuffix = null;
            int dotIdx = rawKey.lastIndexOf('.');
            if (dotIdx > 0) {
                String suffix = rawKey.substring(dotIdx + 1);
                if ("b".equals(suffix) || "i".equals(suffix) || "d".equals(suffix)) {
                    key = rawKey.substring(0, dotIdx);
                    typeSuffix = suffix;
                }
            }

            if ("b".equals(typeSuffix) ||
                    (typeSuffix == null && ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)))) {
                result.addProperty(key, Boolean.parseBoolean(value));
            } else if ("i".equals(typeSuffix)) {
                try {
                    result.addProperty(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    result.addProperty(key, value);
                }
            } else if ("d".equals(typeSuffix)) {
                try {
                    result.addProperty(key, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    result.addProperty(key, value);
                }
            } else {
                // No type suffix — try numeric, fall back to string
                try {
                    int intVal = Integer.parseInt(value);
                    result.addProperty(key, intVal);
                } catch (NumberFormatException e1) {
                    try {
                        double dblVal = Double.parseDouble(value);
                        result.addProperty(key, dblVal);
                    } catch (NumberFormatException e2) {
                        result.addProperty(key, value);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void extractFromYaml(String yamlContent, JsonObject result) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object parsed = yaml.load(yamlContent);
            if (parsed instanceof Map) {
                flattenMap((Map<String, Object>) parsed, "", result);
            }
        } catch (Exception e) {
            // Not valid YAML, try key=value parsing
            for (String line : yamlContent.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    result.addProperty(line.substring(0, eq).trim(),
                            line.substring(eq + 1).trim());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void flattenMap(Map<String, Object> map, String prefix, JsonObject result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                // For nested maps, keep as nested structure for ParameterApplier
                JsonObject nested = new JsonObject();
                flattenMap((Map<String, Object>) value, "", nested);
                result.add(key, nested);
            } else if (value instanceof Boolean) {
                result.addProperty(key, (Boolean) value);
            } else if (value instanceof Number) {
                result.addProperty(key, (Number) value);
            } else if (value != null) {
                result.addProperty(key, value.toString());
            }
        }
    }
}
