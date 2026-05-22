package com.blog.utils;

import com.blog.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    /** Minimum key length in bytes required by HS256 (RFC 7518 §3.2). */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Fail fast at startup if the JWT configuration is unusable, rather than waiting for
     * the first login attempt to blow up.
     */
    @PostConstruct
    void validateConfiguration() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "blog.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (UTF-8). " +
                "Set the BLOG_JWT_SECRET environment variable, or configure it in application-prod.yml. " +
                "Generate with: openssl rand -base64 48"
            );
        }
        if (jwtProperties.getExpireSeconds() <= 0) {
            throw new IllegalStateException("blog.jwt.expire-seconds must be a positive integer");
        }
    }

    public String generateToken(String username) {
        return generateToken(username, "ADMIN");
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());

        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expireAt))
            .signWith(signingKey())
            .compact();
    }

    public String parseUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String parseRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
