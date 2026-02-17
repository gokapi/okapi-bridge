package com.gokapi.bridge.util;

import com.gokapi.bridge.model.FilterInfo;
import net.sf.okapi.common.filters.IFilter;

import java.util.*;

/**
 * Registry of Okapi filters.
 * Dynamically discovers filters from Okapi's DefaultFilters.properties
 * (bundled in okapi-core) at runtime, plus checks for extension filters
 * that are distributed separately from okapi-core.
 */
public class FilterRegistry {

    private static final Map<String, FilterInfo> FILTERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * Extension filters that are NOT listed in DefaultFilters.properties
     * but are commonly available as separate okapi-filter-* JARs.
     * These are checked if available on the classpath.
     */
    private static final String[] EXTENSION_FILTERS = {
            "net.sf.okapi.filters.epub.EpubFilter",
            "net.sf.okapi.filters.wsxzpackage.WsxzPackageFilter",
            "net.sf.okapi.filters.cascadingfilter.CascadingFilter",
            "net.sf.okapi.filters.versifiedtext.VersifiedTextFilter"
    };

    /**
     * Discover all filters from Okapi's DefaultFilters.properties plus
     * extension filters that may be on the classpath.
     */
    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        Set<String> filterClasses = new TreeSet<>();

        // 1. Read from DefaultFilters.properties (bundled in okapi-core)
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.okapi.common.filters.DefaultFilters");
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.startsWith("filterClass_")) {
                    filterClasses.add(bundle.getString(key));
                }
            }
        } catch (MissingResourceException e) {
            System.err.println("[bridge] Could not find DefaultFilters.properties");
        }

        // 2. Add extension filters
        filterClasses.addAll(Arrays.asList(EXTENSION_FILTERS));

        // 3. Check availability and create FilterInfo for each
        for (String filterClass : filterClasses) {
            if (isFilterAvailable(filterClass)) {
                FilterInfo info = createFilterInfo(filterClass);
                if (info != null) {
                    FILTERS.put(filterClass, info);
                }
            }
        }

        System.out.println("[bridge] Discovered " + FILTERS.size() + " available filters from Okapi");
    }

    /**
     * Check if a filter class is available on the classpath.
     */
    private static boolean isFilterAvailable(String filterClass) {
        try {
            Class.forName(filterClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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

            return new FilterInfo(
                    filterClass,
                    formatId,
                    displayName != null ? displayName : name,
                    mimeTypes,
                    Collections.emptyList() // Extensions will be empty - not critical for schema generation
            );
        } catch (Exception e) {
            System.err.println("[bridge] Could not create FilterInfo for " + filterClass + ": " + e.getMessage());
            return null;
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
