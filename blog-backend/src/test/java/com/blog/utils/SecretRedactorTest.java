package com.blog.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretRedactorTest {

    @Test
    void replaces_openai_style_keys() {
        String body = "Error: invalid api key sk-AbcdEFGH1234567890abcdEFGH";

        assertThat(SecretRedactor.redact(body))
            .doesNotContain("sk-AbcdEFGH")
            .contains(SecretRedactor.PLACEHOLDER);
    }

    @Test
    void replaces_bearer_tokens_in_authorization_dumps() {
        String body = "401 Unauthorized: header was Authorization: Bearer abcdef123456789012345";

        String redacted = SecretRedactor.redact(body);

        assertThat(redacted)
            .doesNotContain("Bearer abcdef")
            .contains(SecretRedactor.PLACEHOLDER);
    }

    @Test
    void replaces_jwts() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9eXh.eyJzdWIiOiJhZG1pbiJ9foo.signaturePartXXXXXXXX";
        String text = "token=" + jwt + " trailing";

        String redacted = SecretRedactor.redact(text);

        assertThat(redacted).doesNotContain("eyJ");
        assertThat(redacted).contains(SecretRedactor.PLACEHOLDER);
    }

    @Test
    void leaves_short_random_strings_alone() {
        // Likely not a credential - shorter than the pattern's tail length.
        assertThat(SecretRedactor.redact("hello sk-abc world"))
            .isEqualTo("hello sk-abc world");
    }

    @Test
    void exact_match_redacts_unknown_shape_secrets() {
        String secret = "totally-custom-key-shape-NoNotJWT-NoSk";

        String body = "Provider error 500: " + secret + " is invalid";

        assertThat(SecretRedactor.redact(body, secret))
            .doesNotContain(secret)
            .contains(SecretRedactor.PLACEHOLDER);
    }

    @Test
    void exact_match_ignores_too_short_secrets() {
        // A 4-char "secret" would damage normal text if blindly replaced.
        String body = "abcd appears many times: abcd abcd abcd";

        assertThat(SecretRedactor.redact(body, "abcd"))
            .isEqualTo(body);
    }

    @Test
    void null_and_empty_pass_through_untouched() {
        assertThat(SecretRedactor.redact(null)).isNull();
        assertThat(SecretRedactor.redact("")).isEmpty();
        assertThat(SecretRedactor.redact(null, "anything")).isNull();
        assertThat(SecretRedactor.redact("", "anything")).isEmpty();
    }
}
