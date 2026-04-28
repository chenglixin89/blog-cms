package com.blog.dto;

import java.time.LocalDateTime;

public record AiCallLogResponse(
    Long id,
    String providerName,
    String model,
    String skillCode,
    String skillName,
    String inputPreview,
    String outputPreview,
    Integer success,
    String errorMessage,
    Long elapsedMs,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens,
    LocalDateTime createdAt
) {
}
