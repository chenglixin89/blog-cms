package com.blog.dto;

import java.time.LocalDateTime;

public record MediaCategoryResponse(
    Long id,
    String name,
    String code,
    String description,
    Integer sortOrder,
    Integer assetCount,
    Integer isSystem,
    LocalDateTime updatedAt
) {}