package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class AiSkillRequest {
    @NotBlank(message = "Skill name is required")
    private String name;
    @NotBlank(message = "Skill code is required")
    private String code;
    private String description;
    private String scene;
    @NotBlank(message = "Prompt template is required")
    private String promptTemplate;
    private Integer enabled;
    private Integer sortOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
