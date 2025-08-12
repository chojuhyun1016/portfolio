package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.algorithm.hasher.Argon2Hasher;
import org.example.order.core.infra.crypto.algorithm.hasher.BcryptHasher;
import org.example.order.core.infra.crypto.algorithm.hasher.Sha256Hasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hasher 계열 (Argon2 / Bcrypt / SHA-256) 단위 테스트
 */
class HasherTest {

    private final String rawPassword = "securePassword!123";

    @Test
    @DisplayName("Argon2 해시/검증")
    void testArgon2Hash() {
        Hasher hasher = new Argon2Hasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }

    @Test
    @DisplayName("Bcrypt 해시/검증")
    void testBcryptHash() {
        Hasher hasher = new BcryptHasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }

    @Test
    @DisplayName("SHA-256 해시/검증")
    void testSha256Hash() {
        Hasher hasher = new Sha256Hasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }
}
