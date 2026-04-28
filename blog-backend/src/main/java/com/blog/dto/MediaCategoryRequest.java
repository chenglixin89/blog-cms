package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MediaCategoryRequest {
    @NotBlank(message = "Media category name is required")
    @Size(max = 50, message = "Media category name is too long")
    private String name;

    @Size(max = 40, message = "Media category code is too long")
    private String code;

    @Size(max = 255, message = "Media category description is too long")
    private String description;

    private Integer sortOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}