package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the rate-limit interceptor for {@code /api/admin/login}.
 *
 * <p>Kept in its own {@code WebMvcConfigurer} so it composes cleanly with
 * other configurers (CORS, the admin JWT interceptor, etc.) without forcing
 * any of them to know about each other.</p>
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final LoginRateLimitInterceptor loginRateLimitInterceptor;

    public RateLimitConfig(LoginRateLimitInterceptor loginRateLimitInterceptor) {
        this.loginRateLimitInterceptor = loginRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor)
            .addPathPatterns("/api/admin/login");
    }
}
