package com.blog.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES = 5;

    @Test
    void below_threshold_is_not_blocked() {
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, fixedClock());

        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            limiter.recordFailure("203.0.113.5");
        }

        assertThat(limiter.isBlocked("203.0.113.5")).isFalse();
    }

    @Test
    void at_threshold_is_blocked() {
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, fixedClock());

        for (int i = 0; i < MAX_FAILURES; i++) {
            limiter.recordFailure("203.0.113.5");
        }

        assertThat(limiter.isBlocked("203.0.113.5")).isTrue();
    }

    @Test
    void successful_login_clears_the_counter() {
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, fixedClock());

        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            limiter.recordFailure("203.0.113.5");
        }
        limiter.recordSuccess("203.0.113.5");

        // The counter is reset, so we should be able to fail another (max-1) times.
        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            limiter.recordFailure("203.0.113.5");
        }
        assertThat(limiter.isBlocked("203.0.113.5")).isFalse();
    }

    @Test
    void window_resets_after_it_elapses() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2024-01-01T00:00:00Z"));
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, clock);

        for (int i = 0; i < MAX_FAILURES; i++) {
            limiter.recordFailure("203.0.113.5");
        }
        assertThat(limiter.isBlocked("203.0.113.5")).isTrue();

        clock.advance(WINDOW.plusSeconds(1));

        assertThat(limiter.isBlocked("203.0.113.5")).isFalse();
    }

    @Test
    void counters_for_different_keys_are_independent() {
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, fixedClock());

        for (int i = 0; i < MAX_FAILURES; i++) {
            limiter.recordFailure("198.51.100.7");
        }

        assertThat(limiter.isBlocked("198.51.100.7")).isTrue();
        assertThat(limiter.isBlocked("203.0.113.5")).isFalse();
    }

    @Test
    void blank_or_null_keys_are_silently_ignored() {
        LoginRateLimiter limiter = new LoginRateLimiter(MAX_FAILURES, WINDOW, fixedClock());

        // Should never throw and never count.
        for (int i = 0; i < MAX_FAILURES * 2; i++) {
            limiter.recordFailure(null);
            limiter.recordFailure("");
            limiter.recordFailure("   ");
        }
        assertThat(limiter.isBlocked(null)).isFalse();
        assertThat(limiter.isBlocked("")).isFalse();
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    private static final class AdjustableClock extends Clock {
        private Instant now;

        private AdjustableClock(Instant initial) {
            this.now = initial;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
