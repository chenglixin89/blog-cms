package com.blog.config;

import com.blog.common.ApiResponse;
import com.blog.utils.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Validates the admin JWT for every {@code /api/admin/**} request (except the login endpoint).
 *
 * <p>On success, the resolved admin username and role are exposed as request attributes
 * ({@link #ATTR_ADMIN_USERNAME}, {@link #ATTR_ADMIN_ROLE}) so downstream services can use
 * them without re-parsing the {@code Authorization} header.</p>
 *
 * <p>On failure, a JSON {@link ApiResponse} with HTTP 401 is written directly to the response.</p>
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_USERNAME = "blog.adminUsername";
    public static final String ATTR_ADMIN_ROLE = "blog.adminRole";

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // CORS preflight requests should never be challenged.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token.isBlank() || !jwtTokenProvider.isTokenValid(token)) {
            return writeUnauthorized(response, "Admin token is missing or invalid");
        }

        String role = jwtTokenProvider.parseRole(token);
        if (!ADMIN_ROLE.equals(role)) {
            return writeUnauthorized(response, "Admin token is missing or invalid");
        }

        String username = jwtTokenProvider.parseUsername(token);
        if (username == null || username.isBlank()) {
            return writeUnauthorized(response, "Admin token is missing or invalid");
        }

        request.setAttribute(ATTR_ADMIN_USERNAME, username);
        request.setAttribute(ATTR_ADMIN_ROLE, role);
        return true;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return "";
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private boolean writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.fail(HttpStatus.UNAUTHORIZED.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
