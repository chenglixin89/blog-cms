package com.blog.dto;

import java.time.LocalDateTime;

public record AiSkillResponse(
    Long id,
    String name,
    String code,
    String description,
    String scene,
    String promptTemplate,
    Integer enabled,
    Integer sortOrder,
    LocalDateTime updatedAt
) {
}
