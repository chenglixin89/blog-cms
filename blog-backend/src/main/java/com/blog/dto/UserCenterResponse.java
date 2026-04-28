package com.blog.dto;

import java.util.List;

public record UserCenterResponse(
    FrontUserProfileResponse profile,
    UserActivityStatsResponse stats,
    List<ArticleResponse> favorites,
    List<ArticleResponse> likes,
    List<CommentResponse> comments
) {
}
