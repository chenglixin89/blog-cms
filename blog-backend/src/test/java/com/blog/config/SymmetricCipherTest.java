package com.blog.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link SymmetricCipher}. No Spring context, no DB, no Docker.
 */
class SymmetricCipherTest {

    private static final String SECRET = "this-is-a-test-secret-key-that-is-at-least-32-bytes";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-also-32-bytes-or-more";

    @Test
    void plaintext_round_trips_through_encrypt_decrypt() {
        SymmetricCipher cipher = newCipher(SECRET);

        String plaintext = "sk-secret-api-key-with-some-payload";
        String encrypted = cipher.encrypt(plaintext);

        assertThat(encrypted).startsWith(SymmetricCipher.VERSION_PREFIX);
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void same_plaintext_produces_different_ciphertexts_due_to_random_iv() {
        SymmetricCipher cipher = newCipher(SECRET);

        String first = cipher.encrypt("payload");
        String second = cipher.encrypt("payload");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("payload");
        assertThat(cipher.decrypt(second)).isEqualTo("payload");
    }

    @Test
    void empty_and_null_are_passed_through_unchanged() {
        SymmetricCipher cipher = newCipher(SECRET);

        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isEmpty();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isEmpty();
    }

    @Test
    void legacy_plaintext_is_returned_unchanged_on_decrypt() {
        SymmetricCipher cipher = newCipher(SECRET);

        // No "enc1:" prefix -> assumed to be a legacy row written before encryption was introduced.
        assertThat(cipher.decrypt("plaintext-api-key")).isEqualTo("plaintext-api-key");
        assertThat(cipher.isEncrypted("plaintext-api-key")).isFalse();
    }

    @Test
    void ciphertext_from_a_different_key_cannot_be_decrypted() {
        SymmetricCipher attacker = newCipher(OTHER_SECRET);
        String foreign = attacker.encrypt("payload");

        SymmetricCipher victim = newCipher(SECRET);

        assertThatThrownBy(() -> victim.decrypt(foreign))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AES-GCM decryption failed");
    }

    @Test
    void tampered_ciphertext_is_rejected_by_the_gcm_authentication_tag() {
        SymmetricCipher cipher = newCipher(SECRET);
        String original = cipher.encrypt("payload");

        // Flip a single base64 character in the body to corrupt the tag.
        StringBuilder mutated = new StringBuilder(original);
        int targetIndex = original.length() - 4;
        char ch = mutated.charAt(targetIndex);
        mutated.setCharAt(targetIndex, ch == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> cipher.decrypt(mutated.toString()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AES-GCM decryption failed");
    }

    @Test
    void initialization_fails_on_short_secret() {
        SymmetricCipher cipher = new SymmetricCipher("too-short");

        assertThatThrownBy(cipher::initialize)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("blog.crypto.secret");
    }

    @Test
    void isEncrypted_only_returns_true_for_versioned_blobs() {
        SymmetricCipher cipher = newCipher(SECRET);

        assertThat(cipher.isEncrypted(cipher.encrypt("foo"))).isTrue();
        assertThat(cipher.isEncrypted("foo")).isFalse();
        assertThat(cipher.isEncrypted("")).isFalse();
        assertThat(cipher.isEncrypted(null)).isFalse();
    }

    private static SymmetricCipher newCipher(String secret) {
        SymmetricCipher cipher = new SymmetricCipher(secret);
        cipher.initialize();
        return cipher;
    }
}
