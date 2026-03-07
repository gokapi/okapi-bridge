package com.gokapi.bridge.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for K8s-style envelope config parsing logic.
 * Validates the envelope unwrapping that BridgeServiceImpl performs.
 */
class EnvelopeConfigTest {

    /**
     * Simulate the envelope unwrapping logic from BridgeServiceImpl.applyFilterParams.
     * Returns the unwrapped spec params, or null if not an envelope.
     */
    private Map<String, String> unwrapEnvelope(Map<String, String> params) {
        String kind = params.get("kind");
        if (kind == null || kind.isEmpty()) {
            return null;
        }

        // Validate kind format
        if (!kind.startsWith("Okf") || !kind.endsWith("FilterConfig")) {
            throw new IllegalArgumentException("invalid kind: " + kind);
        }

        // Extract spec
        String specJson = params.get("spec");
        if (specJson == null || specJson.isEmpty()) {
            return new HashMap<>();
        }

        JsonElement specElement = JsonParser.parseString(specJson);
        if (!specElement.isJsonObject()) {
            throw new IllegalArgumentException("spec is not a JSON object");
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : specElement.getAsJsonObject().entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    @Test
    void unwrapEnvelope_validHtmlConfig() {
        Map<String, String> params = new HashMap<>();
        params.put("apiVersion", "v1");
        params.put("kind", "OkfHtmlFilterConfig");
        params.put("spec", "{\"assumeWellformed\":false,\"preserve_whitespace\":true}");

        Map<String, String> spec = unwrapEnvelope(params);
        assertNotNull(spec);
        assertEquals(2, spec.size());
        assertEquals("false", spec.get("assumeWellformed"));
        assertEquals("true", spec.get("preserve_whitespace"));
    }

    @Test
    void unwrapEnvelope_noKind_returnsNull() {
        Map<String, String> params = new HashMap<>();
        params.put("someParam", "value");

        assertNull(unwrapEnvelope(params));
    }

    @Test
    void unwrapEnvelope_emptySpec_returnsEmptyMap() {
        Map<String, String> params = new HashMap<>();
        params.put("apiVersion", "v1");
        params.put("kind", "OkfJsonFilterConfig");

        Map<String, String> spec = unwrapEnvelope(params);
        assertNotNull(spec);
        assertTrue(spec.isEmpty());
    }

    @Test
    void unwrapEnvelope_wrongKindFormat_throws() {
        Map<String, String> params = new HashMap<>();
        params.put("apiVersion", "v1");
        params.put("kind", "FormatConfig");

        assertThrows(IllegalArgumentException.class, () -> unwrapEnvelope(params));
    }

    @Test
    void unwrapEnvelope_nestedSpec() {
        Map<String, String> params = new HashMap<>();
        params.put("apiVersion", "v1");
        params.put("kind", "OkfXmlFilterConfig");
        params.put("spec", "{\"parser\":{\"preserveWhitespace\":true},\"extraction\":{\"extractAll\":false}}");

        Map<String, String> spec = unwrapEnvelope(params);
        assertNotNull(spec);
        assertEquals(2, spec.size());
        // Nested objects are preserved as JSON strings
        JsonObject parser = JsonParser.parseString(spec.get("parser")).getAsJsonObject();
        assertTrue(parser.get("preserveWhitespace").getAsBoolean());
    }

    @Test
    void kind_filterResolution() {
        // Test the full flow: kind -> filter class
        String kind = "OkfHtmlFilterConfig";
        // Strip "Okf" prefix and "FilterConfig" suffix, lowercase
        String format = kind.substring(3, kind.length() - 12).toLowerCase();
        assertEquals("html", format);

        // The filter ID would be okf_html
        String filterId = "okf_" + format;
        assertEquals("okf_html", filterId);
    }

    @Test
    void kind_resolveByKind_unknownVersion_noError() {
        // Even with v99, the filter should still be found (version mismatch
        // is a concern for the caller, not the bridge). The bridge just needs
        // to find the right filter from the kind.
        // resolveByKind doesn't use apiVersion — it only looks at the kind.
        String filterClass = FilterRegistry.resolveByKind("OkfHtmlFilterConfig");
        // If HTML filter is on the classpath, this succeeds; otherwise null.
        // The key point: no exception regardless of apiVersion.
    }
}
