# syntax=docker/dockerfile:1.7
#
# Multi-stage build for the blog-backend Spring Boot 3 application.
#
# Layer order is tuned so that pom.xml changes invalidate the dependency
# cache, but pure source changes only re-trigger the package step.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Resolve dependencies first; copying just pom.xml keeps this layer cache hot
# whenever only Java sources change.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests -q package && \
    cp target/blog-backend-*.jar /build/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="blog-backend" \
      org.opencontainers.image.source="https://github.com/chenglixin89/blog-cms" \
      org.opencontainers.image.description="Spring Boot 3 backend for the blog-cms personal blog." \
      org.opencontainers.image.licenses="MIT"

# Non-root runtime user.
RUN groupadd --system --gid 1000 blog && \
    useradd --system --uid 1000 --gid blog --home /app --shell /sbin/nologin blog

WORKDIR /app
COPY --from=builder --chown=blog:blog /build/app.jar /app/app.jar
RUN install -d -o blog -g blog /app/uploads

USER blog
EXPOSE 8080

# UseContainerSupport + MaxRAMPercentage make the JVM honour the container's
# cgroup memory limit instead of guessing from the host.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
