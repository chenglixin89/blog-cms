package com.blog.dto;

public record DashboardStatsResponse(
    long articleCount,
    long publishedCount,
    long draftCount,
    long categoryCount,
    long tagCount,
    long pendingCommentCount
) {
}
