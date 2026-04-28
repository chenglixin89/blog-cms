package com.blog.dto;

import jakarta.validation.constraints.NotNull;

public class AdminUserStatusRequest {
    @NotNull(message = "Status is required")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
