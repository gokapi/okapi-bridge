package com.gokapi.bridge.tools;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.ParameterDescriptor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.uidescription.*;

import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Introspects Okapi filter Parameters classes to extract parameter metadata.
 * 
 * This handles three types of parameter patterns used in Okapi:
 * 1. StringParameters subclasses - key-value based with typed accessors
 * 2. AbstractMarkupParameters - YAML-based TaggedFilterConfiguration
 * 3. Direct field access - public fields or getter/setter pairs
 * 
 * Also extracts descriptions from:
 * - getParametersDescription() method
 * - IEditorDescriptionProvider.createEditorDescription() for UI metadata
 * - ISimplifierRulesParameters interface constants
 */
public class ParameterIntrospector {

    /**
     * Information about a single parameter.
     */
    public static class ParamInfo {
        public String name;
        public String type;          // "boolean", "string", "integer", "object", "array"
        public Object defaultValue;
        public String description;
        public String displayName;
        public boolean deprecated;
        public String okapiFormat;   // For complex types like codeFinderRules
        public List<String> enumValues;  // For enum parameters
        public String[] enumLabels;      // Display labels for enum values
        
        // UI metadata from EditorDescription
        public String widget;           // "checkbox", "text", "select", "spin", "codeFinder", "path", "folder"
        public String masterParam;      // Parameter that enables/disables this one
        public boolean enabledOnMasterSelected;  // True if enabled when master is selected
        public Integer minimum;         // For spin/integer inputs
        public Integer maximum;         // For spin/integer inputs
        
        public ParamInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
     * Determine the serialization format used by a filter's parameters.
     * Returns "stringParameters" for #v1 key=value format, "yaml" for YAML-based.
     */
    public String getSerializationFormat(String filterClass) {
        try {
            Class<?> clazz = Class.forName(filterClass);
            IFilter filter = (IFilter) clazz.getDeclaredConstructor().newInstance();
            IParameters params = filter.getParameters();
            if (params instanceof StringParameters) {
                return "stringParameters";
            }
            return "yaml";
        } catch (Exception e) {
            return "stringParameters"; // default assumption
        }
    }

    /**
     * Introspect a filter class to extract its parameter metadata.
     * 
     * @param filterClass Fully-qualified filter class name
     * @return Map of parameter name to ParamInfo, or null if introspection fails
     */
    public Map<String, ParamInfo> introspect(String filterClass) {
        try {
            Class<?> clazz = Class.forName(filterClass);
            
            // Get the filter instance to access its parameters
            IFilter filter = (IFilter) clazz.getDeclaredConstructor().newInstance();
            IParameters params = filter.getParameters();
            
            if (params == null) {
                return null;
            }
            
            Map<String, ParamInfo> result = new LinkedHashMap<>();
            
            // Introspect based on parameter class type
            Class<?> paramsClass = params.getClass();
            
            if (params instanceof StringParameters) {
                introspectStringParameters(paramsClass, (StringParameters) params, result);
            } else {
                // For other types, use getter/setter introspection
                introspectByAccessors(paramsClass, params, result);
                // For YAML-based configs (AbstractMarkupParameters), also introspect the YAML blob
                introspectYamlConfig(params, result);
            }
            
            // Extract descriptions from ParametersDescription if available
            enrichWithParametersDescription(paramsClass, params, result);
            
            // Extract UI metadata from EditorDescription if available
            if (params instanceof IEditorDescriptionProvider) {
                enrichWithEditorDescription(paramsClass, (IEditorDescriptionProvider) params, result);
            }
            
            // Add parameters from ISimplifierRulesParameters if implemented
            if (params instanceof ISimplifierRulesParameters) {
                addSimplifierRulesParams((ISimplifierRulesParameters) params, result);
            }
            
            return result;
            
        } catch (ClassNotFoundException e) {
            System.err.println("Filter class not found: " + filterClass);
            return null;
        } catch (Exception e) {
            System.err.println("Failed to introspect " + filterClass + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Enrich parameter info with UI metadata from createEditorDescription().
     */
    private void enrichWithEditorDescription(Class<?> paramsClass, IEditorDescriptionProvider provider,
                                             Map<String, ParamInfo> result) {
        try {
            // First get ParametersDescription (required for createEditorDescription)
            Method descMethod = paramsClass.getMethod("getParametersDescription");
            ParametersDescription paramDesc = (ParametersDescription) descMethod.invoke(provider);
            
            if (paramDesc == null) {
                return;
            }
            
            // Now get EditorDescription
            EditorDescription editorDesc = provider.createEditorDescription(paramDesc);
            if (editorDesc == null) {
                return;
            }
            
            // Process each UI part
            for (Map.Entry<String, AbstractPart> entry : editorDesc.getDescriptors().entrySet()) {
                String paramName = entry.getKey();
                AbstractPart part = entry.getValue();
                
                ParamInfo info = result.get(paramName);
                if (info == null) {
                    // Parameter discovered via editor but not in our result set - try to add it
                    info = extractParamInfo(paramsClass, (IParameters) provider, paramName);
                    if (info != null) {
                        result.put(paramName, info);
                    } else {
                        continue;
                    }
                }
                
                // Extract widget type
                info.widget = mapPartToWidget(part);
                
                // Extract master/slave relationship
                AbstractPart masterPart = part.getMasterPart();
                if (masterPart != null) {
                    info.masterParam = masterPart.getName();
                    info.enabledOnMasterSelected = part.isEnabledOnSelection();
                }
                
                // Extract display name if not already set
                if (info.displayName == null && part.getDisplayName() != null) {
                    info.displayName = part.getDisplayName();
                }
                
                // Extract description if not already set
                if (info.description == null && part.getShortDescription() != null) {
                    info.description = part.getShortDescription();
                }
                
                // Extract type-specific metadata
                if (part instanceof ListSelectionPart) {
                    ListSelectionPart listPart = (ListSelectionPart) part;
                    String[] choices = listPart.getChoicesValues();
                    if (choices != null && choices.length > 0) {
                        info.enumValues = Arrays.asList(choices);
                        info.enumLabels = listPart.getChoicesLabels();
                    }
                } else if (part instanceof SpinInputPart) {
                    SpinInputPart spinPart = (SpinInputPart) part;
                    int min = spinPart.getMinimumValue();
                    int max = spinPart.getMaximumValue();
                    if (min != Integer.MIN_VALUE) {
                        info.minimum = min;
                    }
                    if (max != Integer.MAX_VALUE) {
                        info.maximum = max;
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // No getParametersDescription method
        } catch (Exception e) {
            // Ignore errors in editor description extraction
        }
    }
    
    /**
     * Map an AbstractPart subclass to a widget type string.
     */
    private String mapPartToWidget(AbstractPart part) {
        if (part instanceof CheckboxPart) {
            return "checkbox";
        } else if (part instanceof TextInputPart) {
            return "text";
        } else if (part instanceof ListSelectionPart) {
            ListSelectionPart listPart = (ListSelectionPart) part;
            return listPart.getListType() == ListSelectionPart.LISTTYPE_DROPDOWN ? "dropdown" : "select";
        } else if (part instanceof SpinInputPart) {
            return "spin";
        } else if (part instanceof CodeFinderPart) {
            return "codeFinder";
        } else if (part instanceof PathInputPart) {
            return "path";
        } else if (part instanceof FolderInputPart) {
            return "folder";
        } else if (part instanceof CheckListPart) {
            return "checkList";
        } else if (part instanceof SeparatorPart) {
            return "separator";
        } else if (part instanceof TextLabelPart) {
            return "label";
        }
        return null;
    }
    
    /**
     * Enrich parameter info with descriptions from getParametersDescription().
     */
    private void enrichWithParametersDescription(Class<?> paramsClass, IParameters params,
                                                  Map<String, ParamInfo> result) {
        try {
            Method descMethod = paramsClass.getMethod("getParametersDescription");
            ParametersDescription desc = (ParametersDescription) descMethod.invoke(params);
            
            if (desc != null) {
                for (Map.Entry<String, ParameterDescriptor> entry : desc.getDescriptors().entrySet()) {
                    String paramName = entry.getKey();
                    ParameterDescriptor pd = entry.getValue();
                    
                    ParamInfo info = result.get(paramName);
                    if (info != null) {
                        // Enrich existing parameter with description
                        if (pd.getDisplayName() != null) {
                            info.displayName = pd.getDisplayName();
                        }
                        if (pd.getShortDescription() != null) {
                            info.description = pd.getShortDescription();
                        }
                    } else {
                        // Parameter discovered via description but not found by field scan
                        // Try to determine its type and add it
                        info = extractParamInfo(paramsClass, params, paramName);
                        if (info != null) {
                            if (pd.getDisplayName() != null) {
                                info.displayName = pd.getDisplayName();
                            }
                            if (pd.getShortDescription() != null) {
                                info.description = pd.getShortDescription();
                            }
                            result.put(paramName, info);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // No getParametersDescription method - that's OK
        } catch (Exception e) {
            // Ignore other errors
        }
    }
    
    /**
     * Add simplifier rules parameters from the ISimplifierRulesParameters interface.
     */
    private void addSimplifierRulesParams(ISimplifierRulesParameters params,
                                          Map<String, ParamInfo> result) {
        // simplifierRules - the main parameter from this interface
        String simplifierRulesKey = "simplifierRules";
        if (!result.containsKey(simplifierRulesKey)) {
            ParamInfo info = new ParamInfo(simplifierRulesKey, "string");
            info.displayName = "Simplifier Rules";
            info.description = "Simplifier Rules as defined in the Okapi Code Simplifier Rule Format";
            try {
                info.defaultValue = params.getSimplifierRules();
            } catch (Exception e) {
                // Ignore
            }
            result.put(simplifierRulesKey, info);
        }
    }

    /**
     * Introspect a StringParameters subclass by examining its constant fields
     * and getter/setter methods.
     */
    private void introspectStringParameters(Class<?> paramsClass, StringParameters params,
                                            Map<String, ParamInfo> result) {
        
        // Find all static final String fields in the class hierarchy (these are parameter names)
        // Include package-private and protected fields, not just private
        Set<String> paramNames = new LinkedHashSet<>();
        
        // Scan current class and all superclasses
        Class<?> currentClass = paramsClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers()) &&
                    field.getType() == String.class) {
                    
                    field.setAccessible(true);
                    try {
                        String paramName = (String) field.get(null);
                        // Filter out non-parameter constants (usually all caps with underscores are params)
                        if (paramName != null && !paramName.isEmpty() && 
                            !paramName.contains(" ") && !paramName.startsWith("#")) {
                            paramNames.add(paramName);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        // Also scan implemented interfaces for parameter name constants
        for (Class<?> iface : paramsClass.getInterfaces()) {
            for (Field field : iface.getDeclaredFields()) {
                // Interface fields are implicitly public static final
                if (field.getType() == String.class) {
                    try {
                        String paramName = (String) field.get(null);
                        if (paramName != null && !paramName.isEmpty() && 
                            !paramName.contains(" ") && !paramName.startsWith("#") &&
                            !paramName.endsWith("_DESC") && !paramName.endsWith("_NAME")) {
                            paramNames.add(paramName);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // For each parameter name, find its getter to determine type and default
        for (String paramName : paramNames) {
            ParamInfo info = extractParamInfo(paramsClass, params, paramName);
            if (info != null) {
                result.put(paramName, info);
            }
        }

        // Also introspect public instance fields (used by table/plaintext filters)
        // These are fields like: public String fieldDelimiter, public boolean trimLeading
        introspectPublicInstanceFields(paramsClass, params, result);

        // Also check for complex objects like InlineCodeFinder
        introspectComplexFields(paramsClass, params, result);
    }

    /**
     * Extract parameter info by finding the getter method.
     */
    private ParamInfo extractParamInfo(Class<?> paramsClass, IParameters params, String paramName) {
        // Try common getter patterns
        String[] getterPrefixes = {"get", "is"};
        String camelName = toCamelCase(paramName);
        
        for (String prefix : getterPrefixes) {
            String methodName = prefix + Character.toUpperCase(camelName.charAt(0)) + camelName.substring(1);
            
            try {
                Method getter = paramsClass.getMethod(methodName);
                Class<?> returnType = getter.getReturnType();
                
                ParamInfo info = new ParamInfo(paramName, mapJavaType(returnType));
                
                // Get default value
                try {
                    // Reset to defaults and get value
                    params.reset();
                    info.defaultValue = getter.invoke(params);
                } catch (Exception e) {
                    // Default value unknown
                }
                
                // Extract description from Javadoc (if available via annotation)
                info.description = extractDescription(getter);
                
                return info;
                
            } catch (NoSuchMethodException e) {
                // Try next pattern
            }
        }

        // If no getter found, try to determine type from IParameters interface
        try {
            // Try getBoolean
            params.reset();
            boolean boolVal = params.getBoolean(paramName);
            ParamInfo info = new ParamInfo(paramName, "boolean");
            info.defaultValue = boolVal;
            return info;
        } catch (Exception e) {
            // Not a boolean
        }

        try {
            // Try getString
            params.reset();
            String strVal = params.getString(paramName);
            if (strVal != null) {
                ParamInfo info = new ParamInfo(paramName, "string");
                info.defaultValue = strVal;
                return info;
            }
        } catch (Exception e) {
            // Not a string
        }

        try {
            // Try getInteger
            params.reset();
            int intVal = params.getInteger(paramName);
            ParamInfo info = new ParamInfo(paramName, "integer");
            info.defaultValue = intVal;
            return info;
        } catch (Exception e) {
            // Not an integer
        }

        // Unknown type - default to string
        ParamInfo info = new ParamInfo(paramName, "string");
        return info;
    }

    /**
     * Introspect public instance fields as parameters.
     * Many Okapi filters (table, plaintext) use public fields directly for configuration.
     */
    private void introspectPublicInstanceFields(Class<?> paramsClass, IParameters params,
                                                 Map<String, ParamInfo> result) {
        // Reset to get default values
        try {
            params.reset();
        } catch (Exception e) {
            // Ignore reset failures
        }
        
        // Internal fields that are not user-configurable parameters
        Set<String> internalFields = new HashSet<>(java.util.Arrays.asList(
            "data", "path", "parametersClass", "defParametersClass", 
            "codeFinder", "logger", "LOGGER", "parentFilter"
        ));
        
        // Scan current class and all superclasses for public instance fields
        Class<?> currentClass = paramsClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Skip static fields, we want instance fields
                if (Modifier.isStatic(field.getModifiers())) continue;
                
                // Skip already processed parameters
                String fieldName = field.getName();
                if (result.containsKey(fieldName)) continue;
                
                // Skip internal fields
                if (internalFields.contains(fieldName)) continue;
                
                // Skip non-configurable types
                Class<?> fieldType = field.getType();
                if (!isConfigurableType(fieldType)) continue;
                
                field.setAccessible(true);
                try {
                    ParamInfo info = new ParamInfo(fieldName, mapJavaType(fieldType));
                    
                    // Get default value
                    Object defaultVal = field.get(params);
                    if (defaultVal != null) {
                        // Handle enum fields
                        if (fieldType.isEnum()) {
                            info.defaultValue = defaultVal.toString();
                            info.enumValues = new java.util.ArrayList<>();
                            for (Object c : fieldType.getEnumConstants()) {
                                info.enumValues.add(c.toString());
                            }
                        } else {
                            info.defaultValue = defaultVal;
                        }
                    }
                    
                    result.put(fieldName, info);
                } catch (Exception e) {
                    // Skip fields that can't be accessed
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }
    
    /**
     * Check if a field type is a configurable parameter type.
     */
    private boolean isConfigurableType(Class<?> type) {
        return type == String.class ||
               type == boolean.class || type == Boolean.class ||
               type == int.class || type == Integer.class ||
               type == long.class || type == Long.class ||
               type == double.class || type == Double.class ||
               type == float.class || type == Float.class ||
               type.isEnum();
    }

    /**
     * Look for complex fields like InlineCodeFinder.
     */
    private void introspectComplexFields(Class<?> paramsClass, IParameters params,
                                         Map<String, ParamInfo> result) {
        
        for (Field field : paramsClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            
            field.setAccessible(true);
            
            try {
                if (field.getType() == InlineCodeFinder.class) {
                    // This is handled via codeFinderRules string parameter
                    // Mark the corresponding parameter as needing transformation
                    ParamInfo existing = result.get("codeFinderRules");
                    if (existing != null) {
                        existing.okapiFormat = "inlineCodeFinder";
                        existing.type = "object";  // Will be transformed to clean object format
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }


	// Properties that come from the AbstractMarkupParameters wrapper, not the YAML config
	private static final Set<String> YAML_INTERNAL_PROPS = new HashSet<>(Arrays.asList(
		"taggedConfig", "editorTitle", "path", "data"
	));

	/**
	 * Introspect YAML-based filter configurations (AbstractMarkupParameters / TaggedFilterConfiguration).
	 * Parses the default YAML config to extract top-level properties with their types.
	 */
	@SuppressWarnings("unchecked")
	private void introspectYamlConfig(IParameters params, Map<String, ParamInfo> result) {
		try {
			// Reset to get defaults, then serialize to YAML string
			params.reset();
			String yamlStr = params.toString();
			if (yamlStr == null || yamlStr.isEmpty()) return;

			// Parse YAML to Map
			Yaml yaml = new Yaml();
			Object parsed = yaml.load(yamlStr);
			if (!(parsed instanceof Map)) return;

			Map<String, Object> config = (Map<String, Object>) parsed;

			for (Map.Entry<String, Object> entry : config.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				// Skip properties already discovered by accessor introspection
				if (result.containsKey(key)) continue;
				// Skip internal wrapper properties
				if (YAML_INTERNAL_PROPS.contains(key)) continue;

				if (value instanceof Boolean) {
					ParamInfo info = new ParamInfo(key, "boolean");
					info.defaultValue = value;
					result.put(key, info);
				} else if (value instanceof Integer || value instanceof Long) {
					ParamInfo info = new ParamInfo(key, "integer");
					info.defaultValue = value;
					result.put(key, info);
				} else if (value instanceof Number) {
					ParamInfo info = new ParamInfo(key, "number");
					info.defaultValue = value;
					result.put(key, info);
				} else if (value instanceof String) {
					ParamInfo info = new ParamInfo(key, "string");
					info.defaultValue = value;
					result.put(key, info);
				} else if (value instanceof Map) {
					// Complex map properties like "elements" and "attributes"
					ParamInfo info = new ParamInfo(key, "object");
					info.defaultValue = value;
					if ("elements".equals(key)) {
						info.okapiFormat = "elementRules";
					} else if ("attributes".equals(key)) {
						info.okapiFormat = "attributeRules";
					}
					result.put(key, info);
				} else if (value instanceof List) {
					ParamInfo info = new ParamInfo(key, "array");
					info.defaultValue = value;
					result.put(key, info);
				}
			}
		} catch (Exception e) {
			// YAML parsing failed - skip silently
			System.err.println("YAML config introspection failed: " + e.getMessage());
		}
	}

    /**
     * Introspect by examining getter/setter methods.
     */
    private void introspectByAccessors(Class<?> paramsClass, IParameters params,
                                       Map<String, ParamInfo> result) {
        
        Set<String> processedNames = new HashSet<>();
        
        for (Method method : paramsClass.getMethods()) {
            String name = method.getName();
            
            // Skip Object methods and internal methods
            if (method.getDeclaringClass() == Object.class) continue;
            if (name.equals("getClass")) continue;
            
            // Find getters (getXxx or isXxx with no parameters)
            if (method.getParameterCount() == 0 && !method.getReturnType().equals(void.class)) {
                String paramName = null;
                
                if (name.startsWith("get") && name.length() > 3) {
                    paramName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2 &&
                           method.getReturnType() == boolean.class) {
                    paramName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                
                if (paramName != null && !processedNames.contains(paramName)) {
                    // Check if there's a corresponding setter
                    String setterName = "set" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
                    try {
                        paramsClass.getMethod(setterName, method.getReturnType());
                        // Has setter - this is a configurable parameter
                        
                        ParamInfo info = new ParamInfo(paramName, mapJavaType(method.getReturnType()));
                        
                        try {
                            params.reset();
                            info.defaultValue = method.invoke(params);
                        } catch (Exception e) {
                            // Default unknown
                        }
                        
                        info.description = extractDescription(method);
                        result.put(paramName, info);
                        processedNames.add(paramName);
                        
                    } catch (NoSuchMethodException e) {
                        // No setter - probably not a configurable parameter
                    }
                }
            }
        }
    }

    /**
     * Map Java types to JSON Schema types.
     */
    private String mapJavaType(Class<?> javaType) {
        if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        } else if (javaType == int.class || javaType == Integer.class ||
                   javaType == long.class || javaType == Long.class) {
            return "integer";
        } else if (javaType == float.class || javaType == Float.class ||
                   javaType == double.class || javaType == Double.class) {
            return "number";
        } else if (javaType == String.class) {
            return "string";
        } else if (javaType.isArray() || Collection.class.isAssignableFrom(javaType)) {
            return "array";
        } else if (javaType.isEnum()) {
            return "string";  // Enums become strings with enum constraint
        } else if (javaType == Optional.class) {
            // Optional<T> — we can't resolve T at runtime via Class alone,
            // but in Okapi it's always Optional<Boolean>
            return "boolean";
        } else {
            return "object";
        }
    }

    /**
     * Convert underscore/lowercase name to camelCase.
     */
    private String toCamelCase(String name) {
        // Most Okapi parameter names are already camelCase
        return name;
    }

    /**
     * Extract description from method annotations or Javadoc.
     */
    private String extractDescription(Method method) {
        // In the future, we could use annotation processors or parse Javadoc
        // For now, return null - descriptions will come from editorHints
        return null;
    }
}
