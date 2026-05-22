package com.blog.service;

import com.blog.config.AdminAuthProperties;
import com.blog.dto.AdminPasswordChangeRequest;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.utils.JwtTokenProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AdminAuthProperties adminAuthProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public AuthService(AdminAuthProperties adminAuthProperties, JwtTokenProvider jwtTokenProvider, JdbcTemplate jdbcTemplate) {
        this.adminAuthProperties = adminAuthProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoginResponse login(LoginRequest request) {
        AdminCredential credential = findCredential(request.getUsername());
        boolean passwordMatch = credential != null && matchesPassword(request.getPassword(), credential.passwordHash());

        if (!passwordMatch) {
            throw new ResponseStatusException(UNAUTHORIZED, "Username or password is incorrect");
        }

        String token = jwtTokenProvider.generateToken(credential.username(), ADMIN_ROLE);
        return new LoginResponse(token, defaultText(credential.nickname(), adminAuthProperties.getNickname()));
    }

    /**
     * Changes the password of the currently authenticated admin.
     *
     * <p>The {@code username} is provided by {@link com.blog.config.AdminAuthInterceptor}, which has
     * already validated the JWT before the controller was invoked.</p>
     */
    @Transactional
    public void changePassword(String username, AdminPasswordChangeRequest request) {
        if (username == null || username.isBlank()) {
            // Defensive guard: should never happen because the interceptor blocks unauthenticated calls.
            throw new ResponseStatusException(UNAUTHORIZED, "Admin token is invalid");
        }
        AdminCredential credential = findCredential(username);
        if (credential == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Admin account not found");
        }
        if (!matchesPassword(request.getOldPassword(), credential.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from old password");
        }

        jdbcTemplate.update(
            "UPDATE blog_admin_account SET password_hash = ?, updated_at = NOW() WHERE username = ?",
            bCryptPasswordEncoder.encode(request.getNewPassword()),
            credential.username()
        );
    }

    private AdminCredential findCredential(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                "SELECT username, password_hash, nickname FROM blog_admin_account WHERE username = ? LIMIT 1",
                (rs, rowNum) -> new AdminCredential(
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("nickname")
                ),
                username.trim()
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (storedPassword.startsWith("{noop}")) {
            return storedPassword.substring("{noop}".length()).equals(rawPassword);
        }
        return bCryptPasswordEncoder.matches(rawPassword, storedPassword);
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null || fallback.isBlank() ? "Admin" : fallback;
        }
        return value;
    }

    private record AdminCredential(String username, String passwordHash, String nickname) {
    }
}
