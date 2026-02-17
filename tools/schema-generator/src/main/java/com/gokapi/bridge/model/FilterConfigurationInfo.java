package com.gokapi.bridge.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Metadata about a filter configuration (a specific preset/variant of a filter).
 * Filters can have multiple configurations with different default parameters.
 */
public class FilterConfigurationInfo {

    @SerializedName("configId")
    private String configId;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("extensions")
    private String extensions;

    @SerializedName("parametersLocation")
    private String parametersLocation;

    @SerializedName("parametersRaw")
    private String parametersRaw;

    @SerializedName("parameters")
    private Map<String, Object> parameters;

    @SerializedName("isDefault")
    private boolean isDefault;

    public FilterConfigurationInfo() {
    }

    public FilterConfigurationInfo(String configId, String name, String description,
                                   String mimeType, String extensions, 
                                   String parametersLocation, boolean isDefault) {
        this.configId = configId;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.extensions = extensions;
        this.parametersLocation = parametersLocation;
        this.isDefault = isDefault;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtensions() {
        return extensions;
    }

    public void setExtensions(String extensions) {
        this.extensions = extensions;
    }

    public String getParametersLocation() {
        return parametersLocation;
    }

    public void setParametersLocation(String parametersLocation) {
        this.parametersLocation = parametersLocation;
    }

    public String getParametersRaw() {
        return parametersRaw;
    }

    public void setParametersRaw(String parametersRaw) {
        this.parametersRaw = parametersRaw;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
