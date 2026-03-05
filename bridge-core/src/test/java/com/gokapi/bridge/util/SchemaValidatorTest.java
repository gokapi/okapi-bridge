package com.gokapi.bridge.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {

    private JsonObject makeSchema(JsonObject properties) {
        JsonObject schema = new JsonObject();
        schema.add("properties", properties);
        return schema;
    }

    @Test
    void validate_validBooleanProperty_noErrors() {
        JsonObject props = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        props.add("enabled", boolProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("enabled", true);

        SchemaValidator.ValidationResult result = validator.validate(config);
        assertTrue(result.isValid());
    }

    @Test
    void validate_wrongType_returnsError() {
        JsonObject props = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        props.add("enabled", boolProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("enabled", "not-a-bool");

        SchemaValidator.ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Expected boolean"));
    }

    @Test
    void validate_stringBooleanAccepted() {
        JsonObject props = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        props.add("enabled", boolProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("enabled", "true");

        SchemaValidator.ValidationResult result = validator.validate(config);
        assertTrue(result.isValid(), "String 'true' should be accepted for boolean");
    }

    @Test
    void validate_unknownProperty_returnsWarning() {
        JsonObject props = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        props.add("enabled", boolProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("enabled", true);
        config.addProperty("unknown", "value");

        SchemaValidator.ValidationResult result = validator.validate(config);
        assertTrue(result.isValid(), "Unknown props should be warnings, not errors");
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Unknown"));
    }

    @Test
    void validate_enumConstraint_rejectsInvalidValue() {
        JsonObject props = new JsonObject();
        JsonObject enumProp = new JsonObject();
        enumProp.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("auto");
        enumValues.add("manual");
        enumProp.add("enum", enumValues);
        props.add("mode", enumProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("mode", "invalid");

        SchemaValidator.ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("not in enum"));
    }

    @Test
    void validate_enumConstraint_acceptsValidValue() {
        JsonObject props = new JsonObject();
        JsonObject enumProp = new JsonObject();
        enumProp.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("auto");
        enumValues.add("manual");
        enumProp.add("enum", enumValues);
        props.add("mode", enumProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("mode", "auto");

        assertTrue(validator.validate(config).isValid());
    }

    @Test
    void validate_nestedObject_validatesRecursively() {
        JsonObject innerProps = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        innerProps.add("extractAll", boolProp);

        JsonObject groupProp = new JsonObject();
        groupProp.addProperty("type", "object");
        groupProp.add("properties", innerProps);

        JsonObject props = new JsonObject();
        props.add("extraction", groupProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));

        // Valid nested config
        JsonObject inner = new JsonObject();
        inner.addProperty("extractAll", true);
        JsonObject config = new JsonObject();
        config.add("extraction", inner);
        assertTrue(validator.validate(config).isValid());

        // Invalid nested config
        JsonObject badInner = new JsonObject();
        badInner.addProperty("extractAll", "not-a-bool");
        JsonObject badConfig = new JsonObject();
        badConfig.add("extraction", badInner);
        SchemaValidator.ValidationResult result = validator.validate(badConfig);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("extraction.extractAll"));
    }

    @Test
    void validate_refResolution_validatesReferencedDef() {
        // Build a schema with $defs and $ref
        JsonObject innerProps = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        innerProps.add("enabled", boolProp);

        JsonObject def = new JsonObject();
        def.addProperty("type", "object");
        def.add("properties", innerProps);

        JsonObject defs = new JsonObject();
        defs.add("inlineCodes", def);

        JsonObject refProp = new JsonObject();
        refProp.addProperty("$ref", "#/$defs/inlineCodes");

        JsonObject props = new JsonObject();
        props.add("inlineCodes", refProp);

        JsonObject schema = new JsonObject();
        schema.add("properties", props);
        schema.add("$defs", defs);

        SchemaValidator validator = new SchemaValidator(schema);

        JsonObject inner = new JsonObject();
        inner.addProperty("enabled", true);
        JsonObject config = new JsonObject();
        config.add("inlineCodes", inner);

        assertTrue(validator.validate(config).isValid());
    }

    @Test
    void validate_integerProperty_acceptsStringNumber() {
        JsonObject props = new JsonObject();
        JsonObject intProp = new JsonObject();
        intProp.addProperty("type", "integer");
        props.add("maxDepth", intProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        JsonObject config = new JsonObject();
        config.addProperty("maxDepth", "42");

        assertTrue(validator.validate(config).isValid(),
                "String '42' should be accepted for integer");
    }

    @Test
    void validate_emptyConfig_noErrors() {
        JsonObject props = new JsonObject();
        JsonObject boolProp = new JsonObject();
        boolProp.addProperty("type", "boolean");
        props.add("optional", boolProp);

        SchemaValidator validator = new SchemaValidator(makeSchema(props));
        assertTrue(validator.validate(new JsonObject()).isValid());
    }
}
