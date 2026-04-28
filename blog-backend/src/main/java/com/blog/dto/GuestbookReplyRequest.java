package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class GuestbookReplyRequest {

    @NotBlank(message = "Reply content is required")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
