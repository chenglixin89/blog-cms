package com.blog;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for integration tests that need a real MySQL.
 *
 * <p>The container is declared {@code static} so JUnit + Testcontainers reuse
 * the same instance across every test class that extends this one. Combined
 * with Spring Boot's test context caching, this means MySQL only starts once
 * per {@code mvn test} invocation rather than once per class.</p>
 *
 * <p>Requires Docker on the host. See the project README for the CI setup.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource") // managed by Testcontainers/JUnit lifecycle
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("blog_cms_test")
        .withUsername("test")
        .withPassword("test")
        .withCommand(
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_unicode_ci"
        );
}
