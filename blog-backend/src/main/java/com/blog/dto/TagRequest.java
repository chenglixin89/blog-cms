package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class TagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;

    private String color;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
