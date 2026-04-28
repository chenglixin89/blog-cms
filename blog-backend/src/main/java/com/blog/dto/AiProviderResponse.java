package com.blog.dto;

import java.time.LocalDateTime;

public record AiProviderResponse(
    Long id,
    String name,
    String baseUrl,
    String maskedApiKey,
    String model,
    Double temperature,
    Integer maxTokens,
    Integer enabled,
    LocalDateTime updatedAt
) {
}
