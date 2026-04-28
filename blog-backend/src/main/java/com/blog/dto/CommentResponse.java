package com.blog.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long articleId,
    Long parentId,
    String articleTitle,
    String nickname,
    String email,
    String content,
    Integer status,
    LocalDateTime createdAt
) {
}
