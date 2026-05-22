package com.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String[] allowedOrigins;

    public CorsConfig(
        @Value("${blog.upload.dir:uploads}") String uploadDir,
        @Value("${blog.cors.allowed-origins:}") String allowedOriginsCsv
    ) {
        this.uploadDir = uploadDir;
        this.allowedOrigins = parseAllowedOrigins(allowedOriginsCsv);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();
        if (!uploadLocation.endsWith("/")) {
            uploadLocation = uploadLocation + "/";
        }
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadLocation);
    }

    private static String[] parseAllowedOrigins(String csv) {
        if (csv == null || csv.isBlank()) {
            // No origins configured: deny by default. Dev profile overrides this with "*".
            return new String[0];
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }
}
