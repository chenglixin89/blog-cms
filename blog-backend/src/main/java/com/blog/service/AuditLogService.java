package com.blog.service;

import com.blog.dto.AuditLogResponse;
import com.blog.dto.PageResponse;
import com.blog.entity.AuditLog;
import com.blog.mapper.AuditLogMapper;
import com.blog.utils.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AuditLogMapper auditLogMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public AuditLogService(AuditLogMapper auditLogMapper, JwtTokenProvider jwtTokenProvider) {
        this.auditLogMapper = auditLogMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public PageResponse<AuditLogResponse> page(String keyword, String module, String action, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = normalizeText(keyword);
        String normalizedModule = normalizeText(module);
        String normalizedAction = normalizeText(action);

        long total = auditLogMapper.countPage(normalizedKeyword, normalizedModule, normalizedAction);
        List<AuditLogResponse> records = auditLogMapper
            .selectPage(normalizedKeyword, normalizedModule, normalizedAction, offset, normalizedSize)
            .stream()
            .map(this::toResponse)
            .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, total, normalizedPage, normalizedSize, totalPages);
    }

    public void record(HttpServletRequest request, String module, String action, String targetType, Long targetId, String detail) {
        try {
            AuditLog log = new AuditLog();
            log.setOperator(resolveOperator(request));
            log.setModule(requiredText(module, "system"));
            log.setAction(requiredText(action, "operate"));
            log.setTargetType(truncate(defaultText(targetType), 60));
            log.setTargetId(targetId);
            log.setDetail(truncate(defaultText(detail), 1000));
            log.setIp(truncate(resolveIp(request), 64));
            log.setUserAgent(truncate(request == null ? "" : defaultText(request.getHeader("User-Agent")), 500));
            auditLogMapper.insert(log);
        } catch (Exception ignored) {
            // Audit logging should never block the main business operation.
        }
    }

    private String resolveOperator(HttpServletRequest request) {
        if (request == null) {
            return "admin";
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "admin";
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank() || !jwtTokenProvider.isTokenValid(token)) {
            return "admin";
        }
        String role = jwtTokenProvider.parseRole(token);
        if (!ADMIN_ROLE.equals(role)) {
            return "admin";
        }
        String username = jwtTokenProvider.parseUsername(token);
        return username == null || username.isBlank() ? "admin" : truncate(username, 80);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return defaultText(request.getRemoteAddr());
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private String requiredText(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? fallback : truncate(normalized, 60);
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = defaultText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String normalizeDisplayDetail(String detail) {
        String normalized = defaultText(detail);
        if (!normalized.contains("??")) {
            return normalized;
        }

        String url = extractUrl(normalized);
        String size = extractSize(normalized);
        if (!url.isBlank() && !size.isBlank()) {
            String fileName = normalized
                .replace(url, "")
                .replace(size, "")
                .replace("bytes", "")
                .replace("?", "")
                .trim();
            if (!fileName.isBlank()) {
                return "Upload file: " + fileName + ", size: " + size + " bytes, url: " + url;
            }
        }
        return normalized.replaceAll("\\?{2,}", " ").trim();
    }

    private String extractUrl(String detail) {
        int httpIndex = detail.indexOf("http://");
        if (httpIndex < 0) {
            httpIndex = detail.indexOf("https://");
        }
        return httpIndex < 0 ? "" : detail.substring(httpIndex).trim();
    }

    private String extractSize(String detail) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s*bytes").matcher(detail);
        return matcher.find() ? matcher.group(1) : "";
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getOperator(),
            log.getModule(),
            log.getAction(),
            log.getTargetType(),
            log.getTargetId(),
            normalizeDisplayDetail(log.getDetail()),
            log.getIp(),
            log.getUserAgent(),
            log.getCreatedAt()
        );
    }
}
