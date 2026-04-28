package com.blog.dto;

import java.time.LocalDateTime;

public record DailyQuoteResponse(
    boolean enabled,
    String content,
    String source,
    String author,
    String sourceUrl,
    String provider,
    boolean fallback,
    LocalDateTime fetchedAt
) {
}
