package com.blog.dto;

import java.time.LocalDateTime;

public record MediaAssetResponse(
    Long id,
    String url,
    String originalName,
    String fileName,
    String extension,
    String contentType,
    Long size,
    String category,
    LocalDateTime createdAt
) {
}
