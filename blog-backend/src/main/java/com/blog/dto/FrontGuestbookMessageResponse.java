package com.blog.dto;

import java.time.LocalDateTime;

public record FrontGuestbookMessageResponse(
    Long id,
    Long parentId,
    String nickname,
    String content,
    Integer status,
    LocalDateTime createdAt,
    Boolean loggedInUser
) {
}