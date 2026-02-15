package com.gokapi.bridge.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParameterIntrospector.
 */
class ParameterIntrospectorTest {

    private ParameterIntrospector introspector;

    @BeforeEach
    void setUp() {
        introspector = new ParameterIntrospector();
    }

    @Test
    void introspectJsonFilter_shouldExtractParameters() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.json.JSONFilter");
        
        assertNotNull(params, "Should return parameters for JSON filter");
        assertFalse(params.isEmpty(), "Should have at least one parameter");
        
        // JSON filter should have extractStandalone boolean parameter
        assertTrue(params.containsKey("extractStandalone") || params.containsKey("extractAllPairs"),
            "Should have extractStandalone or extractAllPairs parameter");
    }

    @Test
    void introspectHtmlFilter_shouldExtractParameters() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.html.HtmlFilter");
        
        assertNotNull(params, "Should return parameters for HTML filter");
        assertFalse(params.isEmpty(), "Should have parameters");
    }

    @Test
    void introspectPropertiesFilter_shouldExtractParameters() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.properties.PropertiesFilter");
        
        assertNotNull(params, "Should return parameters for Properties filter");
        assertFalse(params.isEmpty(), "Should have parameters");
        
        // Properties filter has useCodeFinder and codeFinderRules
        boolean hasCodeFinder = params.containsKey("useCodeFinder") || 
                               params.containsKey("codeFinderRules");
        assertTrue(hasCodeFinder, "Should have code finder related parameters");
    }

    @Test
    void introspectXliffFilter_shouldExtractParameters() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.xliff.XLIFFFilter");
        
        assertNotNull(params, "Should return parameters for XLIFF filter");
    }

    @Test
    void introspectNonExistentFilter_shouldReturnNull() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("com.nonexistent.Filter");
        
        assertNull(params, "Should return null for non-existent filter");
    }

    @Test
    void paramInfo_shouldHaveCorrectTypes() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.json.JSONFilter");
        
        assertNotNull(params);
        
        for (ParameterIntrospector.ParamInfo info : params.values()) {
            assertNotNull(info.name, "Parameter should have a name");
            assertNotNull(info.type, "Parameter should have a type");
            
            // Type should be a valid JSON Schema type
            assertTrue(
                info.type.equals("boolean") || 
                info.type.equals("string") || 
                info.type.equals("integer") || 
                info.type.equals("number") || 
                info.type.equals("object") ||
                info.type.equals("array"),
                "Type should be a valid JSON Schema type: " + info.type
            );
        }
    }

    @Test
    void introspectYamlFilter_shouldExtractCodeFinderRules() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.yaml.YamlFilter");
        
        assertNotNull(params, "Should return parameters for YAML filter");
        
        // Check for codeFinderRules if present
        ParameterIntrospector.ParamInfo codeFinderRules = params.get("codeFinderRules");
        if (codeFinderRules != null) {
            assertEquals("inlineCodeFinder", codeFinderRules.okapiFormat,
                "codeFinderRules should have okapiFormat hint");
        }
    }

    @Test
    void introspectOpenXmlFilter_shouldExtractParameters() {
        Map<String, ParameterIntrospector.ParamInfo> params = 
            introspector.introspect("net.sf.okapi.filters.openxml.OpenXMLFilter");
        
        assertNotNull(params, "Should return parameters for OpenXML filter");
        assertFalse(params.isEmpty(), "OpenXML filter should have parameters");
    }
}
