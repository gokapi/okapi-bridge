package com.gokapi.bridge.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SchemaTransformer.
 */
class SchemaTransformerTest {

    private SchemaTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new SchemaTransformer();
    }

    @Test
    void transformParameter_booleanParam_shouldHaveCorrectSchema() {
        ParameterIntrospector.ParamInfo boolParam = new ParameterIntrospector.ParamInfo("extractAll", "boolean");
        boolParam.defaultValue = true;
        boolParam.description = "Extract all pairs";
        
        JsonObject propSchema = transformer.transformParameter("extractAll", boolParam);
        
        assertNotNull(propSchema);
        assertEquals("boolean", propSchema.get("type").getAsString());
        assertTrue(propSchema.get("default").getAsBoolean());
        assertEquals("Extract all pairs", propSchema.get("description").getAsString());
    }

    @Test
    void transformParameter_stringParam_shouldHaveCorrectSchema() {
        ParameterIntrospector.ParamInfo strParam = new ParameterIntrospector.ParamInfo("keyPattern", "string");
        strParam.defaultValue = ".*";
        
        JsonObject propSchema = transformer.transformParameter("keyPattern", strParam);
        
        assertNotNull(propSchema);
        assertEquals("string", propSchema.get("type").getAsString());
        assertEquals(".*", propSchema.get("default").getAsString());
    }

    @Test
    void transformParameter_integerParam_shouldHaveCorrectSchema() {
        ParameterIntrospector.ParamInfo intParam = new ParameterIntrospector.ParamInfo("maxDepth", "integer");
        intParam.defaultValue = 10;
        
        JsonObject propSchema = transformer.transformParameter("maxDepth", intParam);
        
        assertNotNull(propSchema);
        assertEquals("integer", propSchema.get("type").getAsString());
        assertEquals(10, propSchema.get("default").getAsInt());
    }

    @Test
    void transformParameter_codeFinderRulesParam_shouldHaveObjectSchema() {
        ParameterIntrospector.ParamInfo cfParam = new ParameterIntrospector.ParamInfo("codeFinderRules", "object");
        cfParam.okapiFormat = "inlineCodeFinder";
        
        JsonObject propSchema = transformer.transformParameter("codeFinderRules", cfParam);
        
        assertNotNull(propSchema);
        assertEquals("object", propSchema.get("type").getAsString());
        assertEquals("inlineCodeFinder", propSchema.get("x-okapiFormat").getAsString());
        
        // Should have nested properties for rules structure
        assertTrue(propSchema.has("properties"), "Should have nested properties");
        JsonObject nestedProps = propSchema.getAsJsonObject("properties");
        assertTrue(nestedProps.has("rules"), "Should have rules property");
        assertTrue(nestedProps.has("sample"), "Should have sample property");
        assertTrue(nestedProps.has("useAllRulesWhenTesting"), "Should have useAllRulesWhenTesting");
    }

    @Test
    void transformParameter_deprecatedParam_shouldBeMarked() {
        ParameterIntrospector.ParamInfo deprecatedParam = new ParameterIntrospector.ParamInfo("oldOption", "boolean");
        deprecatedParam.deprecated = true;
        
        JsonObject propSchema = transformer.transformParameter("oldOption", deprecatedParam);
        
        assertNotNull(propSchema);
        assertTrue(propSchema.get("deprecated").getAsBoolean());
    }

    @Test
    void transformParameter_enumParam_shouldHaveEnumConstraint() {
        ParameterIntrospector.ParamInfo enumParam = new ParameterIntrospector.ParamInfo("mode", "string");
        enumParam.enumValues = java.util.Arrays.asList("auto", "manual", "hybrid");
        
        JsonObject propSchema = transformer.transformParameter("mode", enumParam);
        
        assertNotNull(propSchema);
        assertTrue(propSchema.has("enum"));
        JsonArray enumValues = propSchema.getAsJsonArray("enum");
        assertEquals(3, enumValues.size());
        assertEquals("auto", enumValues.get(0).getAsString());
    }

    @Test
    void transformParameter_subfilterParam_shouldReturnNull() {
        ParameterIntrospector.ParamInfo subfilterParam = new ParameterIntrospector.ParamInfo("subfilter", "string");
        
        JsonObject propSchema = transformer.transformParameter("subfilter", subfilterParam);
        
        // subfilter is handled by gokapi Layers, so should be excluded
        assertNull(propSchema);
    }

    @Test
    void transformParameter_simplifierRules_shouldHaveWidget() {
        ParameterIntrospector.ParamInfo simpParam = new ParameterIntrospector.ParamInfo("simplifierRules", "string");
        
        JsonObject propSchema = transformer.transformParameter("simplifierRules", simpParam);
        
        assertNotNull(propSchema);
        assertEquals("simplifierRulesEditor", propSchema.get("x-widget").getAsString());
    }

    @Test
    void mergeEditorHints_shouldMergeGroups() {
        // Create a schema
        JsonObject schema = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonObject prop1 = new JsonObject();
        prop1.addProperty("type", "boolean");
        properties.add("extractAll", prop1);
        schema.add("properties", properties);
        
        // Create editor hints
        JsonObject hints = new JsonObject();
        JsonArray groups = new JsonArray();
        JsonObject group = new JsonObject();
        group.addProperty("id", "extraction");
        group.addProperty("label", "Extraction Options");
        JsonArray fields = new JsonArray();
        fields.add("extractAll");
        group.add("fields", fields);
        groups.add(group);
        hints.add("groups", groups);
        
        transformer.mergeEditorHints(schema, hints);
        
        assertTrue(schema.has("x-groups"), "Should have x-groups from hints");
        JsonArray xGroups = schema.getAsJsonArray("x-groups");
        assertEquals(1, xGroups.size());
        assertEquals("extraction", xGroups.get(0).getAsJsonObject().get("id").getAsString());
    }

    @Test
    void mergeEditorHints_shouldMergeFieldHints() {
        // Create a schema
        JsonObject schema = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        properties.add("pattern", prop);
        schema.add("properties", properties);
        
        // Create editor hints with field hints
        JsonObject hints = new JsonObject();
        JsonObject fields = new JsonObject();
        JsonObject patternHints = new JsonObject();
        patternHints.addProperty("widget", "regexBuilder");
        patternHints.addProperty("placeholder", "Enter regex pattern");
        patternHints.addProperty("description", "Regular expression for matching");
        fields.add("pattern", patternHints);
        hints.add("fields", fields);
        
        transformer.mergeEditorHints(schema, hints);
        
        JsonObject patternProp = schema.getAsJsonObject("properties").getAsJsonObject("pattern");
        assertEquals("regexBuilder", patternProp.get("x-widget").getAsString());
        assertEquals("Enter regex pattern", patternProp.get("x-placeholder").getAsString());
        assertEquals("Regular expression for matching", patternProp.get("description").getAsString());
    }

    @Test
    void mergeEditorHints_shouldMergePresets() {
        // Create a schema
        JsonObject schema = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "object");
        properties.add("codeFinderRules", prop);
        schema.add("properties", properties);
        
        // Create editor hints with presets
        JsonObject hints = new JsonObject();
        JsonObject fields = new JsonObject();
        JsonObject cfHints = new JsonObject();
        JsonObject presets = new JsonObject();
        JsonObject htmlPreset = new JsonObject();
        JsonArray rules = new JsonArray();
        JsonObject rule = new JsonObject();
        rule.addProperty("pattern", "<[^>]+>");
        rules.add(rule);
        htmlPreset.add("rules", rules);
        presets.add("HTML tags", htmlPreset);
        cfHints.add("presets", presets);
        fields.add("codeFinderRules", cfHints);
        hints.add("fields", fields);
        
        transformer.mergeEditorHints(schema, hints);
        
        JsonObject cfProp = schema.getAsJsonObject("properties").getAsJsonObject("codeFinderRules");
        assertTrue(cfProp.has("x-presets"));
        assertTrue(cfProp.getAsJsonObject("x-presets").has("HTML tags"));
    }

    @Test
    void mergeEditorHints_shouldMergeShowIf() {
        JsonObject schema = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        properties.add("pattern", prop);
        schema.add("properties", properties);
        
        JsonObject hints = new JsonObject();
        JsonObject fields = new JsonObject();
        JsonObject patternHints = new JsonObject();
        JsonObject showIf = new JsonObject();
        showIf.addProperty("field", "useCodeFinder");
        showIf.addProperty("value", true);
        patternHints.add("showIf", showIf);
        fields.add("pattern", patternHints);
        hints.add("fields", fields);
        
        transformer.mergeEditorHints(schema, hints);
        
        JsonObject patternProp = schema.getAsJsonObject("properties").getAsJsonObject("pattern");
        assertTrue(patternProp.has("x-showIf"));
        assertEquals("useCodeFinder", patternProp.getAsJsonObject("x-showIf").get("field").getAsString());
    }
}
