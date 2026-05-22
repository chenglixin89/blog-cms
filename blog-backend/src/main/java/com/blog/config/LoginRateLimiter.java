package com.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory sliding-window failed-login limiter, keyed on client IP.
 *
 * <p>Defaults: 5 failures within 15 minutes triggers a block. Successful
 * logins clear the counter immediately. The limiter is bounded to
 * {@value #MAX_TRACKED_KEYS} keys to keep an attacker from filling the map
 * with random forged headers; expired entries are evicted lazily when the
 * threshold is crossed.</p>
 *
 * <p>Trade-offs: no persistence and no clustering, so a restart resets the
 * state and a multi-instance deployment lets each node count separately.
 * Acceptable for a single-node personal blog; switch to Redis or a real
 * rate-limit library (bucket4j, resilience4j) when scaling out.</p>
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 5_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
        @Value("${blog.rate-limit.login.max-failures:5}") int maxFailures,
        @Value("${blog.rate-limit.login.window-minutes:15}") long windowMinutes
    ) {
        this(maxFailures, Duration.ofMinutes(Math.max(1L, windowMinutes)), Clock.systemUTC());
    }

    /** Test seam: lets unit tests inject a deterministic clock. */
    LoginRateLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = Math.max(1, maxFailures);
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return false;
        }
        if (windowExpired(record)) {
            attempts.remove(key, record);
            return false;
        }
        return record.count >= maxFailures;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        attempts.compute(key, (k, existing) -> {
            Instant now = clock.instant();
            if (existing == null || windowExpired(existing)) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(existing.count + 1, existing.windowStart);
        });
        if (attempts.size() > MAX_TRACKED_KEYS) {
            evictExpired();
        }
    }

    public void recordSuccess(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        attempts.remove(key);
    }

    private boolean windowExpired(AttemptRecord record) {
        return record.windowStart.plus(window).isBefore(clock.instant());
    }

    private void evictExpired() {
        attempts.entrySet().removeIf(entry -> windowExpired(entry.getValue()));
    }

    record AttemptRecord(int count, Instant windowStart) {
    }
}
