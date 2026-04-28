package com.blog.dto;

import java.time.LocalDateTime;

public record FriendLinkResponse(
    Long id,
    String name,
    String url,
    String description,
    String logo,
    Integer status,
    Integer sortOrder,
    LocalDateTime createdAt
) {
}
