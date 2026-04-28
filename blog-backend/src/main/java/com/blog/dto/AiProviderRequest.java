package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class AiProviderRequest {
    @NotBlank(message = "Provider name is required")
    private String name;
    @NotBlank(message = "Base URL is required")
    private String baseUrl;
    private String apiKey;
    @NotBlank(message = "Model is required")
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer enabled;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
