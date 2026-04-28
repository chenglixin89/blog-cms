package com.blog.dto;

import java.util.List;

public class AiSkillRunRequest {
    private String title;
    private String summary;
    private String content;
    private List<String> existingTags;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getExistingTags() { return existingTags; }
    public void setExistingTags(List<String> existingTags) { this.existingTags = existingTags; }
}
