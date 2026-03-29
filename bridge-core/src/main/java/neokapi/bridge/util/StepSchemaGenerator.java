package neokapi.bridge.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.pipeline.BasePipelineStep;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Generates JSON Schema from Okapi step parameters.
 * Introspects the step's @UsingParameters annotation, instantiates the
 * Parameters class, and parses the serialized ParametersString (#v1 format)
 * to discover parameter names and types.
 */
public class StepSchemaGenerator {

    /**
     * Generate a ComponentSchema-compatible JSON object for a step.
     *
     * @param info the StepInfo with step metadata
     * @return JSON Schema object, or null if schema cannot be generated
     */
    public static JsonObject generateSchema(StepInfo info) {
        if (info == null) {
            return null;
        }

        String stepId = "okapi:" + info.deriveStepId();
        String displayName = info.getName() != null ? info.getName() : info.deriveStepId();

        JsonObject schema = new JsonObject();
        schema.addProperty("$id", stepId);
        schema.addProperty("title", displayName);
        schema.addProperty("type", "object");

        // x-component metadata
        JsonObject xComponent = new JsonObject();
        xComponent.addProperty("id", stepId);
        xComponent.addProperty("type", "step");
        xComponent.addProperty("displayName", displayName);
        if (info.getDescription() != null && !info.getDescription().isEmpty()) {
            xComponent.addProperty("description", info.getDescription());
        }
        schema.add("x-component", xComponent);

        // Generate properties from the Parameters class.
        JsonObject properties = generateProperties(info);
        if (properties != null && properties.size() > 0) {
            schema.add("properties", properties);
        }

        return schema;
    }

    /**
     * Generate property schemas from the step's Parameters class.
     */
    private static JsonObject generateProperties(StepInfo info) {
        if (info.getParametersClass() == null) {
            return null;
        }

        try {
            // Instantiate the parameters class.
            Object paramsObj = info.getParametersClass().getDeclaredConstructor().newInstance();
            if (!(paramsObj instanceof IParameters)) {
                return null;
            }
            IParameters params = (IParameters) paramsObj;
            params.reset();

            // Try to get ParametersDescription from the step for display names.
            ParametersDescription paramsDesc = getParametersDescription(info);

            // Get the serialized form (ParametersString #v1 format).
            String serialized = params.toString();
            if (serialized == null || serialized.trim().isEmpty()) {
                return null;
            }

            return parseParametersString(serialized, paramsDesc);
        } catch (Exception e) {
            System.err.println("[bridge] Could not generate properties for step "
                    + info.getClassName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Try to get a ParametersDescription from the step's Parameters object.
     */
    private static ParametersDescription getParametersDescription(StepInfo info) {
        if (info.getParametersClass() == null) {
            return null;
        }
        try {
            Object paramsObj = info.getParametersClass().getDeclaredConstructor().newInstance();
            if (paramsObj instanceof net.sf.okapi.common.IParameters) {
                return ((net.sf.okapi.common.IParameters) paramsObj).getParametersDescription();
            }
        } catch (Exception e) {
            // Not all parameters provide descriptions
        }
        return null;
    }

    /**
     * Parse a ParametersString (#v1 format) to discover parameter names and types.
     * The #v1 format uses lines like:
     *   paramName.b=true     (boolean)
     *   paramName.i=42       (integer)
     *   paramName=value      (string)
     */
    private static JsonObject parseParametersString(String serialized, ParametersDescription paramsDesc) {
        JsonObject properties = new JsonObject();

        String[] lines = serialized.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }

            String rawKey = line.substring(0, eq).trim();
            String rawValue = line.substring(eq + 1).trim();

            // Determine type from suffix.
            String paramName;
            String paramType;
            Object defaultValue;

            if (rawKey.endsWith(".b")) {
                paramName = rawKey.substring(0, rawKey.length() - 2);
                paramType = "boolean";
                defaultValue = Boolean.parseBoolean(rawValue);
            } else if (rawKey.endsWith(".i")) {
                paramName = rawKey.substring(0, rawKey.length() - 2);
                paramType = "integer";
                try {
                    defaultValue = Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    defaultValue = 0;
                }
            } else {
                paramName = rawKey;
                paramType = "string";
                defaultValue = rawValue;
            }

            JsonObject prop = new JsonObject();
            prop.addProperty("type", paramType);

            // Set default value.
            if (defaultValue instanceof Boolean) {
                prop.add("default", new JsonPrimitive((Boolean) defaultValue));
            } else if (defaultValue instanceof Integer) {
                prop.add("default", new JsonPrimitive((Integer) defaultValue));
            } else if (defaultValue instanceof String) {
                prop.add("default", new JsonPrimitive((String) defaultValue));
            }

            // Add description from ParametersDescription if available.
            if (paramsDesc != null) {
                try {
                    net.sf.okapi.common.ParameterDescriptor pd = paramsDesc.get(paramName);
                    if (pd != null) {
                        String shortDesc = pd.getShortDescription();
                        if (shortDesc != null && !shortDesc.isEmpty()) {
                            prop.addProperty("description", shortDesc);
                        }
                        String displayName = pd.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            prop.addProperty("title", displayName);
                        }
                    }
                } catch (Exception e) {
                    // ignore — param may not have a description
                }
            }

            properties.add(paramName, prop);
        }

        return properties;
    }
}
