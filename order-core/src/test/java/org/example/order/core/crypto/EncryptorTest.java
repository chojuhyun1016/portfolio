package org.example.order.core.crypto;

import org.example.order.core.crypto.impl.Aes128Encryptor;
import org.example.order.core.crypto.impl.Aes256Encryptor;
import org.example.order.core.crypto.impl.AesGcmEncryptor;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorTest {

    private static final SecureRandom random = new SecureRandom();

    private String generateRandomBase64Key(int size) {
        byte[] keyBytes = new byte[size];
        random.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }

    @Test
    void testAesGcmEncryptAndDecrypt() {
        String base64Key = generateRandomBase64Key(32); // AES-GCM uses 256-bit key
        Encryptor encryptor = new AesGcmEncryptor(base64Key);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for GCM";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes128EncryptAndDecrypt() {
        String base64Key = generateRandomBase64Key(16); // AES-128 uses 128-bit key
        Encryptor encryptor = new Aes128Encryptor(base64Key);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES128";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        String base64Key = generateRandomBase64Key(32); // AES-256 uses 256-bit key
        Encryptor encryptor = new Aes256Encryptor(base64Key);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES256";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }
}
