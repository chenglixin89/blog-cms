package com.blog.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
    Long id,
    String username,
    String nickname,
    String email,
    String avatar,
    String bio,
    String role,
    Integer status,
    long favoriteCount,
    long likeCount,
    long commentCount,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
