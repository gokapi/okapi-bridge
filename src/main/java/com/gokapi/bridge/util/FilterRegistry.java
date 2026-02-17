package com.gokapi.bridge.util;

import com.gokapi.bridge.model.FilterConfigurationInfo;
import com.gokapi.bridge.model.FilterInfo;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Registry of Okapi filters.
 * Dynamically discovers filters by scanning okapi-filter-* JARs on the classpath
 * for classes ending in "Filter" that implement IFilter.
 */
public class FilterRegistry {

    private static final Map<String, FilterInfo> FILTERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * Discover all filters by scanning the classpath for okapi-filter-* JARs.
     * This approach requires no hardcoded filter lists - any filter JAR on the
     * classpath will be automatically discovered.
     */
    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        Set<String> filterClasses = new TreeSet<>();

        // Scan okapi-filter-* JARs from the classloader
        // Maven exec:java uses URLClassLoader, not system classpath
        ClassLoader cl = FilterRegistry.class.getClassLoader();
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                String path = url.getPath();
                if (path.contains("okapi-filter-") && path.endsWith(".jar")) {
                    scanJarForFilters(path, filterClasses);
                }
            }
        }

        // Also check system classpath (for standalone Java runs)
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String path : classpath.split(File.pathSeparator)) {
                if (path.contains("okapi-filter-") && path.endsWith(".jar")) {
                    scanJarForFilters(path, filterClasses);
                }
            }
        }

        // Check availability and create FilterInfo for each
        for (String filterClass : filterClasses) {
            FilterInfo info = createFilterInfo(filterClass);
            if (info != null) {
                FILTERS.put(filterClass, info);
            }
        }

        System.out.println("[bridge] Discovered " + FILTERS.size() + " available filters from Okapi");
    }

    /**
     * Scan a JAR file for classes that look like Okapi filters.
     * Filters are classes ending in "Filter" in the net.sf.okapi.filters package.
     */
    private static void scanJarForFilters(String jarPath, Set<String> filterClasses) {
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // Look for Filter classes in net/sf/okapi/filters (not inner classes)
                if (name.startsWith("net/sf/okapi/filters/") 
                        && name.endsWith("Filter.class") 
                        && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    filterClasses.add(className);
                }
            }
        } catch (Exception e) {
            System.err.println("[bridge] Could not scan JAR " + jarPath + ": " + e.getMessage());
        }
    }

    /**
     * Create FilterInfo by instantiating the filter and extracting metadata.
     */
    private static FilterInfo createFilterInfo(String filterClass) {
        try {
            Class<?> clazz = Class.forName(filterClass);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof IFilter)) {
                return null;
            }

            IFilter filter = (IFilter) instance;
            String name = filter.getName();
            String displayName = filter.getDisplayName();
            String mimeType = filter.getMimeType();

            // Derive format ID from class name (e.g., "HTMLFilter" -> "html")
            String formatId = deriveFormatId(clazz.getSimpleName());

            List<String> mimeTypes = mimeType != null && !mimeType.isEmpty()
                    ? Collections.singletonList(mimeType)
                    : Collections.emptyList();

            FilterInfo info = new FilterInfo(
                    filterClass,
                    formatId,
                    displayName != null ? displayName : name,
                    mimeTypes,
                    Collections.emptyList()
            );

            // Extract filter configurations (presets/variants)
            List<FilterConfiguration> configs = filter.getConfigurations();
            if (configs != null && !configs.isEmpty()) {
                boolean firstConfig = true;
                for (FilterConfiguration config : configs) {
                    FilterConfigurationInfo configInfo = new FilterConfigurationInfo(
                            config.configId,
                            config.name,
                            config.description,
                            config.mimeType,
                            config.extensions,
                            config.parametersLocation,
                            firstConfig
                    );
                    
                    // Load parameters from file if available
                    if (config.parametersLocation != null && !config.parametersLocation.isEmpty()) {
                        loadParametersForConfig(filter, config.parametersLocation, configInfo);
                    }
                    
                    info.addConfiguration(configInfo);
                    firstConfig = false;
                }
            }

            return info;
        } catch (Exception e) {
            System.err.println("[bridge] Could not create FilterInfo for " + filterClass + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Load parameters from a classpath resource file (.yml or .fprm format).
     */
    private static void loadParametersForConfig(IFilter filter, String parametersLocation, 
                                                 FilterConfigurationInfo configInfo) {
        try {
            InputStream is = filter.getClass().getResourceAsStream(parametersLocation);
            if (is == null) {
                // Try without leading slash
                is = filter.getClass().getResourceAsStream("/" + parametersLocation);
            }
            if (is == null) {
                return;
            }
            
            // Read the raw content
            String raw;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                raw = sb.toString();
            }
            
            configInfo.setParametersRaw(raw);
            
            // Parse based on file extension
            if (parametersLocation.endsWith(".yml") || parametersLocation.endsWith(".yaml")) {
                // Parse YAML
                Yaml yaml = new Yaml();
                Object parsed = yaml.load(raw);
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) parsed;
                    configInfo.setParameters(params);
                }
            } else if (parametersLocation.endsWith(".fprm")) {
                // Parse .fprm (properties-like format)
                Map<String, Object> params = parseFprmFormat(raw);
                if (!params.isEmpty()) {
                    configInfo.setParameters(params);
                }
            }
        } catch (Exception e) {
            // Log but don't fail - parameters are optional enhancement
            System.err.println("[bridge] Could not load parameters from " + parametersLocation + ": " + e.getMessage());
        }
    }
    
    /**
     * Parse Okapi .fprm format (first line is parametersClass=..., rest are key=value).
     */
    private static Map<String, Object> parseFprmFormat(String content) {
        Map<String, Object> params = new LinkedHashMap<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                
                // Skip the parametersClass line - it's metadata, not a parameter
                if ("parametersClass".equals(key)) {
                    params.put("_parametersClass", value);
                    continue;
                }
                
                // Try to parse as boolean/number
                params.put(key, parseValue(value));
            }
        }
        return params;
    }
    
    /**
     * Parse a string value into appropriate type (boolean, int, double, or string).
     */
    private static Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    /**
     * Derive a format ID from the filter class simple name.
     * E.g., "HTMLFilter" -> "html", "OpenXMLFilter" -> "openxml"
     */
    private static String deriveFormatId(String simpleName) {
        String id = simpleName;
        if (id.endsWith("Filter")) {
            id = id.substring(0, id.length() - 6);
        }
        return id.toLowerCase();
    }

    /**
     * Get metadata for a filter class.
     *
     * @param filterClass fully-qualified Java class name
     * @return FilterInfo or null if not found
     */
    public static FilterInfo getFilterInfo(String filterClass) {
        ensureInitialized();
        return FILTERS.get(filterClass);
    }

    /**
     * Create a new instance of the specified filter.
     *
     * @param filterClass fully-qualified Java class name
     * @return new IFilter instance or null
     */
    public static IFilter createFilter(String filterClass) {
        ensureInitialized();
        try {
            Class<?> clazz = Class.forName(filterClass);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof IFilter) {
                return (IFilter) instance;
            }
            return null;
        } catch (Exception e) {
            System.err.println("[bridge] Failed to instantiate filter " + filterClass + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * List all discovered and available filters.
     */
    public static List<FilterInfo> listFilters() {
        ensureInitialized();
        return new ArrayList<>(FILTERS.values());
    }

    /**
     * Get all discovered filter class names.
     */
    public static Set<String> getFilterClasses() {
        ensureInitialized();
        return new LinkedHashSet<>(FILTERS.keySet());
    }
}
