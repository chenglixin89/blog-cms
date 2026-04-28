package com.blog.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    String description,
    Integer sortOrder,
    Integer articleCount,
    LocalDateTime updatedAt
) {
}
