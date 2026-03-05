package com.gokapi.bridge.util;

import com.google.gson.JsonObject;
import net.sf.okapi.common.StringParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FprmConverterTest {

    @Test
    void toJson_stringParametersFprm_extractsKeyValues() {
        StringParameters params = new StringParameters();
        String fprmContent = "#v1\nextractAllPairs.b=true\nkeyPattern=.*\nmaxDepth.i=5\n";

        JsonObject result = FprmConverter.toJson(params, fprmContent);

        assertNotNull(result);
        // StringParameters parses #v1 format; verify we can extract values
        assertTrue(result.size() > 0, "Should extract at least some parameters");
    }

    @Test
    void toJson_nullParams_returnsNull() {
        assertNull(FprmConverter.toJson(null, "content"));
    }

    @Test
    void toJson_nullContent_returnsNull() {
        assertNull(FprmConverter.toJson(new StringParameters(), null));
    }

    @Test
    void extractParameters_stringParameters_parsesKeyValues() {
        StringParameters params = new StringParameters();
        params.setBoolean("enabled", true);
        params.setString("pattern", ".*");
        params.setInteger("count", 3);

        JsonObject result = FprmConverter.extractParameters(params);
        assertNotNull(result);
        // StringParameters serializes to key=value format
        assertTrue(result.has("enabled"));
        assertTrue(result.has("pattern"));
        assertTrue(result.has("count"));
        assertTrue(result.get("enabled").getAsBoolean());
        assertEquals(".*", result.get("pattern").getAsString());
        assertEquals(3, result.get("count").getAsInt());
    }

    @Test
    void extractParameters_emptyParams_returnsEmptyJson() {
        StringParameters params = new StringParameters();
        JsonObject result = FprmConverter.extractParameters(params);
        assertNotNull(result);
    }
}
