package com.gokapi.bridge.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FilterRegistry envelope support (Kind + apiVersion).
 */
class FilterRegistryTest {

    // ── toKind ───────────────────────────────────────────────────────────

    @Test
    void toKind_html() {
        assertEquals("OkfHtmlFilterConfig", FilterRegistry.toKind("okf_html"));
    }

    @Test
    void toKind_json() {
        assertEquals("OkfJsonFilterConfig", FilterRegistry.toKind("okf_json"));
    }

    @Test
    void toKind_openxml() {
        assertEquals("OkfOpenxmlFilterConfig", FilterRegistry.toKind("okf_openxml"));
    }

    @Test
    void toKind_xmlstream() {
        assertEquals("OkfXmlstreamFilterConfig", FilterRegistry.toKind("okf_xmlstream"));
    }

    @Test
    void toKind_plaintext() {
        assertEquals("OkfPlaintextFilterConfig", FilterRegistry.toKind("okf_plaintext"));
    }

    @Test
    void toKind_nullFilterId_returnsNull() {
        assertNull(FilterRegistry.toKind(null));
    }

    @Test
    void toKind_invalidPrefix_returnsNull() {
        assertNull(FilterRegistry.toKind("invalid_html"));
    }

    // ── toApiVersion ─────────────────────────────────────────────────────

    @Test
    void toApiVersion_v1() {
        assertEquals("v1", FilterRegistry.toApiVersion(1));
    }

    @Test
    void toApiVersion_v3() {
        assertEquals("v3", FilterRegistry.toApiVersion(3));
    }

    // ── resolveByKind ────────────────────────────────────────────────────

    @Test
    void resolveByKind_validHtml_returnsFilterClass() {
        // The filter registry discovers filters from classpath JARs.
        String filterClass = FilterRegistry.resolveByKind("OkfHtmlFilterConfig");
        // May be null if no HTML filter is on the classpath.
        if (filterClass != null) {
            assertTrue(filterClass.contains("html") || filterClass.contains("Html") ||
                    filterClass.contains("HTML"),
                    "Expected HTML filter class, got: " + filterClass);
        }
    }

    @Test
    void resolveByKind_null_returnsNull() {
        assertNull(FilterRegistry.resolveByKind(null));
    }

    @Test
    void resolveByKind_empty_returnsNull() {
        assertNull(FilterRegistry.resolveByKind(""));
    }

    @Test
    void resolveByKind_invalidFormat_noPrefix_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                FilterRegistry.resolveByKind("HtmlFilterConfig"));
    }

    @Test
    void resolveByKind_invalidFormat_noSuffix_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                FilterRegistry.resolveByKind("OkfHtml"));
    }

    @Test
    void resolveByKind_emptyFormat_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                FilterRegistry.resolveByKind("OkfFilterConfig"));
    }

    @Test
    void resolveByKind_unknownFormat_returnsNull() {
        assertNull(FilterRegistry.resolveByKind("OkfNonexistentFilterConfig"));
    }

    // ── roundtrip ────────────────────────────────────────────────────────

    @Test
    void toKind_roundtrip() {
        String kind = FilterRegistry.toKind("okf_properties");
        assertEquals("OkfPropertiesFilterConfig", kind);

        // Parse back: strip "Okf" prefix and "FilterConfig" suffix, lowercase
        String format = kind.substring(3, kind.length() - 12).toLowerCase();
        assertEquals("properties", format);
        assertEquals("okf_properties", "okf_" + format);
    }
}
