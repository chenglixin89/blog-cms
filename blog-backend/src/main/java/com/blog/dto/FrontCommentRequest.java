package com.blog.dto;

import jakarta.validation.constraints.NotBlank;

public class FrontCommentRequest {

    private Long parentId;

    @NotBlank(message = "Nickname is required")
    private String nickname;

    private String email;

    @NotBlank(message = "Comment content is required")
    private String content;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
