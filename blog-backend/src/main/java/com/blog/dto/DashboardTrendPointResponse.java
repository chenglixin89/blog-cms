package com.blog.dto;

public record DashboardTrendPointResponse(
    String key,
    String label,
    long value
) {
}