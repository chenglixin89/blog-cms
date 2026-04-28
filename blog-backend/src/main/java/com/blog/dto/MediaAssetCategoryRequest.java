package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class MediaAssetCategoryRequest {
    @NotBlank(message = "Media category is required")
    private String category;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}