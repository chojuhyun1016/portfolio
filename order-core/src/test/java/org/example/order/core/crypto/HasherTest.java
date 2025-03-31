package org.example.order.core.crypto;

import org.example.order.core.crypto.impl.Argon2Hasher;
import org.example.order.core.crypto.impl.BcryptHasher;
import org.example.order.core.crypto.impl.Sha256Hasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HasherTest {

    private final String rawPassword = "securePassword!123";

    @Test
    void testArgon2Hash() {
        Hasher hasher = new Argon2Hasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }

    @Test
    void testBcryptHash() {
        Hasher hasher = new BcryptHasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }

    @Test
    void testSha256Hash() {
        Hasher hasher = new Sha256Hasher();

        assertTrue(hasher.isReady());
        String hash = hasher.hash(rawPassword);
        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("wrongPassword", hash));
    }
}
