package org.example.order.core.infra.crypto;

import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Encryptor 계열 (AES128 / AES256 / AES-GCM) 단위 테스트
 */
class EncryptorTest {

    private SecretsKeyResolver secretsKeyResolver;

    @BeforeEach
    void setup() {
        // 테스트용 SecretsKeyResolver 준비
        this.secretsKeyResolver = new SecretsKeyResolver();

        // 기본 AES-256 키 생성 후 저장 (Secrets Manager처럼)
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES256);
        byte[] decodedKey = Base64.getUrlDecoder().decode(base64Key);

        secretsKeyResolver.updateKey(decodedKey);
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
        Encryptor encryptor = initEncryptor(new Aes128Encryptor(secretsKeyResolver));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES128";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        Encryptor encryptor = initEncryptor(new Aes256Encryptor(secretsKeyResolver));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES256";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAesGcmEncryptAndDecrypt() {
        Encryptor encryptor = initEncryptor(new AesGcmEncryptor(secretsKeyResolver));

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES-GCM";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }
}
