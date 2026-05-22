package com.blog.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM symmetric cipher for encrypting at-rest secrets such as the AI
 * provider API key.
 *
 * <p>Storage format: {@code "enc1:" + base64(iv || ciphertext_with_tag)}.
 * The fixed prefix lets callers tell encrypted values apart from legacy
 * plaintext (handy during the one-shot migration that rewrites old rows).
 * On decrypt, anything without the prefix is returned unchanged so the
 * application keeps working in the brief window before the migration
 * runs against an existing database.</p>
 *
 * <p>The configured {@code blog.crypto.secret} is hashed with SHA-256 to
 * produce the 32-byte AES key, so the secret can be any sufficiently long
 * passphrase rather than a hand-encoded base64 blob. A new 12-byte IV is
 * drawn from {@link SecureRandom} for every encryption, so encrypting the
 * same plaintext twice yields different ciphertext.</p>
 */
@Component
public class SymmetricCipher {

    private static final String CIPHER_TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MIN_SECRET_BYTES = 32;
    static final String VERSION_PREFIX = "enc1:";

    private final String configuredSecret;
    private final SecureRandom random = new SecureRandom();
    private SecretKey key;

    public SymmetricCipher(@Value("${blog.crypto.secret:}") String secret) {
        this.configuredSecret = secret;
    }

    @PostConstruct
    void initialize() {
        byte[] secretBytes = configuredSecret == null
            ? new byte[0]
            : configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "blog.crypto.secret must be at least " + MIN_SECRET_BYTES + " bytes (UTF-8). " +
                "Set the BLOG_CRYPTO_SECRET environment variable. " +
                "Generate one with: openssl rand -base64 48"
            );
        }
        try {
            byte[] derived = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            this.key = new SecretKeySpec(derived, "AES");
        } catch (NoSuchAlgorithmException ex) {
            // Every JDK ships SHA-256; this branch exists only to satisfy the checked exception.
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    /**
     * Encrypts {@code plaintext} to the {@code "enc1:..."} format. {@code null}
     * stays {@code null} and an empty string stays empty so callers do not
     * accidentally store ciphertext for "no value at all".
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (plaintext.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-GCM encryption failed", ex);
        }
    }

    /**
     * Decrypts an {@code "enc1:..."} blob. Values that do not carry the prefix
     * are assumed to be legacy plaintext and are returned untouched, so the
     * service layer can keep reading existing rows during the migration
     * window. Tampered or truncated ciphertext throws.
     */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (stored.isEmpty()) {
            return "";
        }
        if (!stored.startsWith(VERSION_PREFIX)) {
            return stored;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(stored.substring(VERSION_PREFIX.length()));
            if (decoded.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext is too short to contain an IV and tag");
            }
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(decoded, IV_BYTES, decoded.length);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("AES-GCM decryption failed", ex);
        }
    }

    /** {@code true} iff {@code stored} was produced by this cipher (or a previous version with the same prefix). */
    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(VERSION_PREFIX);
    }
}
