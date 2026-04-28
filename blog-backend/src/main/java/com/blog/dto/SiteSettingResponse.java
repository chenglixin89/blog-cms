package com.blog.dto;

import java.time.LocalDateTime;

public record SiteSettingResponse(
    Long id,
    String siteName,
    String siteSubtitle,
    String logo,
    String avatar,
    String heroTitle,
    String heroSubtitle,
    String authorName,
    String authorBio,
    String footerText,
    Integer dailyQuoteEnabled,
    String dailyQuoteApiUrl,
    String roadmapJson,
    LocalDateTime updatedAt
) {
}