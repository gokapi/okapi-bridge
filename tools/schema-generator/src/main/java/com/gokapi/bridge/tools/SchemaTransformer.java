package com.gokapi.bridge.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Transforms parameter metadata into clean JSON Schema format and merges editor hints.
 * 
 * This class handles:
 * 1. Converting complex Okapi internal formats (like InlineCodeFinder) to clean JSON
 * 2. Merging manually-curated editor hints (groupings, widgets, presets)
 * 3. Adding x-extension properties for UI generation
 */
public class SchemaTransformer {

    /**
     * Transform a single parameter into JSON Schema property format.
     * 
     * @param paramName The parameter name
     * @param paramInfo The introspected parameter information
     * @return JSON Schema property object
     */
    public JsonObject transformParameter(String paramName, ParameterIntrospector.ParamInfo paramInfo) {
        JsonObject prop = new JsonObject();
        
        // Handle special Okapi formats
        if ("inlineCodeFinder".equals(paramInfo.okapiFormat)) {
            return transformCodeFinderRules();
        }
        
        if ("simplifierRules".equals(paramName)) {
            return transformSimplifierRules();
        }
        
        // Skip subfilter parameter - handled via gokapi Layers
        if ("subfilter".equals(paramName)) {
            return null;
        }
        
        // Skip UI-only elements (separators, labels with UUID names)
        if ("separator".equals(paramInfo.widget) || "label".equals(paramInfo.widget)) {
            return null;
        }
        // Also skip parameters with UUID-like names (UI separators)
        if (paramName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return null;
        }

        // Basic type mapping
        prop.addProperty("type", paramInfo.type);
        
        // Add default value
        if (paramInfo.defaultValue != null) {
            addDefaultValue(prop, paramInfo.type, paramInfo.defaultValue);
        }
        
        // Add title (displayName) if available
        if (paramInfo.displayName != null && !paramInfo.displayName.isEmpty()) {
            prop.addProperty("title", paramInfo.displayName);
        }
        
        // Add description
        if (paramInfo.description != null && !paramInfo.description.isEmpty()) {
            prop.addProperty("description", paramInfo.description);
        }
        
        // Add enum constraint if applicable
        if (paramInfo.enumValues != null && !paramInfo.enumValues.isEmpty()) {
            JsonArray enumArray = new JsonArray();
            for (String val : paramInfo.enumValues) {
                enumArray.add(val);
            }
            prop.add("enum", enumArray);
            
            // Add enum labels as x-enumLabels if different from values
            if (paramInfo.enumLabels != null && paramInfo.enumLabels.length == paramInfo.enumValues.size()) {
                JsonArray labelsArray = new JsonArray();
                for (String label : paramInfo.enumLabels) {
                    labelsArray.add(label);
                }
                prop.add("x-enumLabels", labelsArray);
            }
        }
        
        // Add numeric constraints for integer types
        if ("integer".equals(paramInfo.type)) {
            if (paramInfo.minimum != null) {
                prop.addProperty("minimum", paramInfo.minimum);
            }
            if (paramInfo.maximum != null) {
                prop.addProperty("maximum", paramInfo.maximum);
            }
        }
        
        // Mark deprecated
        if (paramInfo.deprecated) {
            prop.addProperty("deprecated", true);
        }
        
        // Add widget hint from EditorDescription
        if (paramInfo.widget != null) {
            prop.addProperty("x-widget", paramInfo.widget);
        }
        
        // Add master/slave relationship
        if (paramInfo.masterParam != null) {
            JsonObject dependency = new JsonObject();
            dependency.addProperty("parameter", paramInfo.masterParam);
            dependency.addProperty("enabledWhenSelected", paramInfo.enabledOnMasterSelected);
            prop.add("x-enabledBy", dependency);
        }
        
        return prop;
    }

    /**
     * Transform InlineCodeFinder to clean object schema.
     * 
     * Okapi internal format: "#v1\ncount.i=2\nrule0=<[^>]+>\nrule1=\\{\\d+\\}\nsample=..."
     * Clean gokapi format: { "rules": [{ "pattern": "..." }], "sample": "...", "useAllRulesWhenTesting": true }
     */
    private JsonObject transformCodeFinderRules() {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "object");
        prop.addProperty("description", "Inline code detection configuration");
        
        JsonObject properties = new JsonObject();
        
        // rules array
        JsonObject rulesArray = new JsonObject();
        rulesArray.addProperty("type", "array");
        rulesArray.addProperty("description", "List of regex patterns to detect inline codes");
        
        JsonObject ruleItem = new JsonObject();
        ruleItem.addProperty("type", "object");
        
        JsonObject ruleProps = new JsonObject();
        JsonObject patternProp = new JsonObject();
        patternProp.addProperty("type", "string");
        patternProp.addProperty("description", "Regex pattern for inline code detection");
        ruleProps.add("pattern", patternProp);
        ruleItem.add("properties", ruleProps);
        
        JsonArray required = new JsonArray();
        required.add("pattern");
        ruleItem.add("required", required);
        
        rulesArray.add("items", ruleItem);
        properties.add("rules", rulesArray);
        
        // sample
        JsonObject sampleProp = new JsonObject();
        sampleProp.addProperty("type", "string");
        sampleProp.addProperty("description", "Sample text to test patterns against");
        properties.add("sample", sampleProp);
        
        // useAllRulesWhenTesting
        JsonObject useAllProp = new JsonObject();
        useAllProp.addProperty("type", "boolean");
        useAllProp.addProperty("default", true);
        useAllProp.addProperty("description", "Test all rules together or individually");
        properties.add("useAllRulesWhenTesting", useAllProp);
        
        prop.add("properties", properties);
        prop.addProperty("x-okapiFormat", "inlineCodeFinder");
        
        return prop;
    }

    /**
     * Transform SimplifierRules to clean schema.
     */
    private JsonObject transformSimplifierRules() {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        prop.addProperty("description", "Simplifier rules for code type normalization");
        prop.addProperty("x-widget", "simplifierRulesEditor");
        return prop;
    }

    /**
     * Add a default value to a property schema.
     */
    private void addDefaultValue(JsonObject prop, String type, Object value) {
        switch (type) {
            case "boolean":
                if (value instanceof Boolean) {
                    prop.addProperty("default", (Boolean) value);
                }
                break;
            case "integer":
                if (value instanceof Number) {
                    prop.addProperty("default", ((Number) value).intValue());
                }
                break;
            case "number":
                if (value instanceof Number) {
                    prop.addProperty("default", ((Number) value).doubleValue());
                }
                break;
            case "string":
                if (value instanceof String) {
                    prop.addProperty("default", (String) value);
                }
                break;
            case "array":
                if (value instanceof JsonArray) {
                    prop.add("default", (JsonArray) value);
                }
                break;
            case "object":
                if (value instanceof JsonObject) {
                    prop.add("default", (JsonObject) value);
                }
                break;
        }
    }

    /**
     * Merge editor hints into the schema.
     * 
     * Editor hints provide:
     * - Parameter groupings for UI
     * - Widget type specifications
     * - Presets for common configurations
     * - Enhanced descriptions
     */
    public void mergeEditorHints(JsonObject schema, JsonObject hints) {
        // Merge groups into x-groups extension
        if (hints.has("groups")) {
            schema.add("x-groups", hints.get("groups"));
        }
        
        // Merge field-specific hints into properties
        if (hints.has("fields") && schema.has("properties")) {
            JsonObject fields = hints.getAsJsonObject("fields");
            JsonObject properties = schema.getAsJsonObject("properties");
            
            for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
                String fieldName = entry.getKey();
                JsonObject fieldHints = entry.getValue().getAsJsonObject();
                
                if (properties.has(fieldName)) {
                    JsonObject prop = properties.getAsJsonObject(fieldName);
                    mergeFieldHints(prop, fieldHints);
                }
            }
        }
    }

    /**
     * Merge hints for a single field.
     */
    private void mergeFieldHints(JsonObject prop, JsonObject hints) {
        // Widget type
        if (hints.has("widget")) {
            prop.addProperty("x-widget", hints.get("widget").getAsString());
        }
        
        // Placeholder text
        if (hints.has("placeholder")) {
            prop.addProperty("x-placeholder", hints.get("placeholder").getAsString());
        }
        
        // Presets
        if (hints.has("presets")) {
            prop.add("x-presets", hints.get("presets"));
        }
        
        // Enhanced description (override if provided)
        if (hints.has("description")) {
            prop.addProperty("description", hints.get("description").getAsString());
        }
        
        // Display order
        if (hints.has("order")) {
            prop.addProperty("x-order", hints.get("order").getAsInt());
        }
        
        // Conditional visibility
        if (hints.has("showIf")) {
            prop.add("x-showIf", hints.get("showIf"));
        }
    }
}
