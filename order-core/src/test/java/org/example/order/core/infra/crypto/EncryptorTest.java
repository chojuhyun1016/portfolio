package org.example.order.core.infra.crypto;

import org.example.order.core.infra.common.kms.config.KmsProperties;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorTest {

    private EncryptProperties properties;
    private KmsDecryptor mockKmsDecryptor;

    @BeforeEach
    void setup() {
        // 설정 생성
        properties = new EncryptProperties();

        EncryptProperties.Aes128 aes128 = new EncryptProperties.Aes128();
        EncryptProperties.Aes256 aes256 = new EncryptProperties.Aes256();
        EncryptProperties.AesGcm aesgcm = new EncryptProperties.AesGcm();

        aes128.setKey(EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES128));
        aes256.setKey(EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES256));
        aesgcm.setKey(EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AESGCM));

        properties.setAes128(aes128);
        properties.setAes256(aes256);
        properties.setAesgcm(aesgcm);

        // Mock KMS (base64 디코딩만 수행)
        mockKmsDecryptor = new KmsDecryptor(new KmsProperties()) {
            @Override
            public byte[] decryptBase64EncodedKey(String base64Key) {
                String normalizedKey = base64Key.replace('-', '+').replace('_', '/');
                return Base64.getDecoder().decode(normalizedKey);
            }
        };
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
        Encryptor encryptor = initEncryptor(new AesGcmEncryptor(properties, mockKmsDecryptor));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for GCM";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }
}
