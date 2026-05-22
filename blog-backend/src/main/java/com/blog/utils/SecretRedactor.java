package com.blog.utils;

import java.util.regex.Pattern;

/**
 * Best-effort masking of secret-shaped substrings before a piece of text is
 * persisted to a log row or returned to the client.
 *
 * <p>Why this exists: when an upstream call (typically the LLM provider) fails,
 * the HTTP response body is embedded into an exception message and that
 * message ends up in three places &mdash; the AI call log, the admin audit
 * log, and the JSON 4xx/5xx response body. Some providers cheerfully echo the
 * {@code Authorization} header back in their 401 page, so a single misconfig
 * could otherwise stamp the live API key into all three sinks.</p>
 *
 * <p>The patterns below are intentionally conservative: each one matches
 * something whose <em>shape</em> tells us it almost certainly is a credential
 * (long random suffix, recognised vendor prefix, JWT structure, etc.). The
 * helper is also overloaded with a variant that takes the literal expected
 * secret, which the caller can supply when it knows it &mdash; that path
 * gives us an exact-match redaction even if the provider chose a brand new
 * key shape we have not seen before.</p>
 */
public final class SecretRedactor {

    public static final String PLACEHOLDER = "[REDACTED]";

    /** Minimum literal-secret length we are willing to scrub. Below this it is
     *  almost never a real credential and over-masking would damage debugging. */
    private static final int MIN_LITERAL_LENGTH = 8;

    private static final Pattern[] PATTERNS = new Pattern[]{
        // OpenAI / Anthropic / many vendors: sk- followed by a long random tail.
        Pattern.compile("sk-[A-Za-z0-9_\\-]{16,}"),
        // Bearer <token> appearing inside an echoed Authorization header.
        Pattern.compile("(?i)bearer\\s+[A-Za-z0-9_\\-\\.]{16,}"),
        // Whole "Authorization: ..." lines in dumped headers.
        Pattern.compile("(?i)authorization\\s*[:=]\\s*\\S{16,}"),
        // Anything JWT-shaped (header.payload.signature).
        Pattern.compile("eyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}")
    };

    private SecretRedactor() {
    }

    /** Mask credentials that match any of the built-in shape patterns. */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (Pattern pattern : PATTERNS) {
            result = pattern.matcher(result).replaceAll(PLACEHOLDER);
        }
        return result;
    }

    /**
     * Mask credentials that match the patterns AND any literal occurrence of
     * the supplied {@code exactSecret}. Use this when the caller already
     * knows the value of the secret in play (e.g. the AI provider's API key
     * that was just used for a request) so we can guarantee redaction even
     * if the secret shape is unrecognised.
     */
    public static String redact(String text, String exactSecret) {
        String result = redact(text);
        if (result == null || result.isEmpty()) {
            return result;
        }
        if (exactSecret == null || exactSecret.length() < MIN_LITERAL_LENGTH) {
            return result;
        }
        return result.replace(exactSecret, PLACEHOLDER);
    }
}
