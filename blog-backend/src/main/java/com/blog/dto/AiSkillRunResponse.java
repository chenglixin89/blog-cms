package com.blog.dto;

import java.util.List;

public record AiSkillRunResponse(
    String skillCode,
    String rawText,
    List<String> titles,
    String summary,
    List<String> existingTags,
    List<String> newTags,
    String content,
    String seoTitle,
    String seoDescription,
    List<String> seoKeywords,
    Long elapsedMs
) {
}
