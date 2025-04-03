package org.example.order.core.crypto;

import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.config.EncryptProperties;
import org.example.order.core.crypto.impl.Aes128Encryptor;
import org.example.order.core.crypto.impl.Aes256Encryptor;
import org.example.order.core.crypto.impl.AesGcmEncryptor;
import org.example.order.core.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorTest {

    @Test
    void testAesGcmEncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AESGCM);
        EncryptProperties encryptProperties = new EncryptProperties();
        encryptProperties.getAesgcm().setKey(base64Key);
        Encryptor encryptor = new AesGcmEncryptor(encryptProperties);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for GCM";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes128EncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES128);
        EncryptProperties encryptProperties = new EncryptProperties();
        encryptProperties.getAes128().setKey(base64Key);
        Encryptor encryptor = new Aes128Encryptor(encryptProperties);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES128";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES256);
        EncryptProperties encryptProperties = new EncryptProperties();
        encryptProperties.getAes256().setKey(base64Key);
        Encryptor encryptor = new Aes256Encryptor(encryptProperties);

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES256";

        String encrypted = encryptor.encrypt(plainText);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }
}
