package com.blog.config;

import com.blog.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Throttles {@code POST /api/admin/login} per client IP.
 *
 * <p>The actual counting lives in {@link LoginRateLimiter}. This interceptor
 * is the glue that:
 * <ul>
 *   <li>checks whether the caller is currently blocked before the controller
 *       runs (returning HTTP 429 directly if so), and</li>
 *   <li>after the controller has produced a response, increments the failure
 *       counter when the status is 401 or clears it when the login succeeded
 *       (2xx). Every other status (400 validation, 500 error, ...) leaves the
 *       counter untouched so that benign client mistakes do not lock people
 *       out.</li>
 * </ul>
 */
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitInterceptor.class);

    private final LoginRateLimiter limiter;
    private final ObjectMapper objectMapper;

    public LoginRateLimitInterceptor(LoginRateLimiter limiter, ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // Only enforce on POST so OPTIONS preflight and accidental GETs pass through.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String key = clientIp(request);
        if (limiter.isBlocked(key)) {
            log.warn("Rate-limited login attempt from {}", key);
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed login attempts. Try again in a few minutes.");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        String key = clientIp(request);
        int status = response.getStatus();
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            limiter.recordFailure(key);
        } else if (status >= 200 && status < 300) {
            limiter.recordSuccess(key);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "" : remote.trim();
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(status.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
