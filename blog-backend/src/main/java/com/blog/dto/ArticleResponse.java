package com.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleResponse(
    Long id,
    String title,
    String summary,
    String seoTitle,
    String seoDescription,
    String seoKeywords,
    String content,
    String coverImage,
    Long categoryId,
    String categoryName,
    List<ArticleTagResponse> tags,
    Integer status,
    Integer isPinned,
    Integer viewCount,
    Integer likeCount,
    Integer commentCount,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    LocalDateTime updatedAt
) {
}
