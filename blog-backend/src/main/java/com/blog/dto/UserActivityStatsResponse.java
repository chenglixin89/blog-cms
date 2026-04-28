package com.blog.dto;

public record UserActivityStatsResponse(
    long favoriteCount,
    long likeCount,
    long commentCount,
    long pendingCommentCount,
    long approvedCommentCount
) {
}
