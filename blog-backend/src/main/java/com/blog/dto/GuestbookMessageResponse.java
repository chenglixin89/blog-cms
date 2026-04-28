package com.blog.dto;

import java.time.LocalDateTime;

public record GuestbookMessageResponse(
    Long id,
    Long parentId,
    String nickname,
    String email,
    String content,
    Integer status,
    LocalDateTime createdAt
) {
}
