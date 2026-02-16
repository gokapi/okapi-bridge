package com.gokapi.bridge.schema;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.ParameterDescriptor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

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
 * - IEditorDescriptionProvider interface
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
        
        public ParamInfo(String name, String type) {
            this.name = name;
            this.type = type;
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
            }
            
            // Extract descriptions from ParametersDescription if available
            enrichWithParametersDescription(paramsClass, params, result);
            
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
