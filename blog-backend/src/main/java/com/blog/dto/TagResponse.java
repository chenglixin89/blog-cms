package com.blog.dto;

import java.time.LocalDateTime;

public record TagResponse(
    Long id,
    String name,
    String color,
    Integer articleCount,
    LocalDateTime updatedAt
) {
}
