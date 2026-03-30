package neokapi.bridge.util;

import java.util.List;

/**
 * Metadata about a discovered Okapi pipeline step.
 * Serialized to JSON via Gson for --list-capabilities output.
 */
public class StepInfo {
    private final String className;
    private final String name;
    private final String description;
    private final transient Class<?> parametersClass; // from @UsingParameters, excluded from Gson
    private final String parametersClassName;

    // Enriched metadata for neokapi integration (set after construction)
    private String stepId;
    private String category;
    private List<String> inputs;
    private List<String> outputs;
    private List<String> tags;
    private List<String> requires;

    public StepInfo(String className, String name, String description, Class<?> parametersClass) {
        this.className = className;
        this.name = name;
        this.description = description;
        this.parametersClass = parametersClass;
        this.parametersClassName = parametersClass != null ? parametersClass.getName() : null;
        this.stepId = deriveStepId();
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getParametersClass() {
        return parametersClass;
    }

    public String getParametersClassName() {
        return parametersClassName;
    }

    /**
     * Get the step ID derived from the class name.
     * E.g., "net.sf.okapi.steps.searchandreplace.SearchAndReplaceStep" -> "search-and-replace"
     */
    public String getStepId() {
        return stepId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires;
    }

    /**
     * Derive a step ID from the class name.
     * E.g., "SearchAndReplaceStep" -> "search-and-replace"
     */
    public String deriveStepId() {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        if (simpleName.endsWith("Step")) {
            simpleName = simpleName.substring(0, simpleName.length() - 4);
        }
        // Convert CamelCase to kebab-case
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
