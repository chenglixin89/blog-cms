package com.blog.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String operator,
    String module,
    String action,
    String targetType,
    Long targetId,
    String detail,
    String ip,
    String userAgent,
    LocalDateTime createdAt
) {
}
