package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorTest {

    private final EncryptProperties properties = new EncryptProperties();

    // Mocked KMS decryptor
    private final KmsDecryptor mockKmsDecryptor = new KmsDecryptor(properties) {
        @Override
        public byte[] decryptBase64EncodedKey(String base64Key) {
            String normalizedKey = base64Key.replace('-', '+').replace('_', '/');
            return Base64.getDecoder().decode(normalizedKey);
        }
    };

    @BeforeEach
    void setup() {
        properties.setKmsRegion("ap-northeast-2");
    }

    private Encryptor initEncryptor(Encryptor encryptor) {
        if (encryptor instanceof Aes128Encryptor aes128) {
            aes128.init();
        } else if (encryptor instanceof Aes256Encryptor aes256) {
            aes256.init();
        } else if (encryptor instanceof AesGcmEncryptor aesGcm) {
            aesGcm.init();
        }

        return encryptor;
    }

    @Test
    void testAes128EncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES128);
        properties.getAes128().setKey(base64Key);
        Encryptor encryptor = initEncryptor(new Aes128Encryptor(properties, mockKmsDecryptor));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES128";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES256);
        properties.getAes256().setKey(base64Key);
        Encryptor encryptor = initEncryptor(new Aes256Encryptor(properties, mockKmsDecryptor));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES256";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAesGcmEncryptAndDecrypt() {
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AESGCM);
        properties.getAesgcm().setKey(base64Key);
        Encryptor encryptor = initEncryptor(new AesGcmEncryptor(properties, mockKmsDecryptor));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for GCM";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }
}
