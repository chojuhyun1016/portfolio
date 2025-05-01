package org.example.order.core.infra.crypto;

import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Encryptor 계열 (AES128 / AES256 / AES-GCM) 단위 테스트 - 키매니저1 적용
 */
class EncryptorTest {

    private SecretsKeyResolver secretsKeyResolver;

    @BeforeEach
    void setup() {
        this.secretsKeyResolver = new SecretsKeyResolver();

        // AES-128: 16 bytes key
        byte[] aes128Key = generateKey(16);
        secretsKeyResolver.updateKey(
                CryptoAlgorithmType.AES128.name(),
                createKeySpec("AES-CBC", 128, aes128Key)
        );

        // AES-256: 32 bytes key
        byte[] aes256Key = generateKey(32);
        secretsKeyResolver.updateKey(
                CryptoAlgorithmType.AES256.name(),
                createKeySpec("AES-CBC", 256, aes256Key)
        );

        // AES-GCM: 32 bytes key
        byte[] aesGcmKey = generateKey(32);
        secretsKeyResolver.updateKey(
                CryptoAlgorithmType.AESGCM.name(),
                createKeySpec("AES-GCM", 256, aesGcmKey)
        );
    }

    private CryptoKeySpec createKeySpec(String algorithm, int keySize, byte[] keyBytes) {
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm(algorithm);
        spec.setKeySize(keySize);
        spec.setValue(Base64Utils.encodeUrlSafe(keyBytes));

        return spec;
    }

    private byte[] generateKey(int length) {
        byte[] keyBytes = new byte[length];

        for (int i = 0; i < length; i++) {
            keyBytes[i] = (byte) (i + 1);  // 간단한 테스트용 키 생성
        }

        return keyBytes;
    }

    @Test
    void testAes128EncryptAndDecrypt() {
        Aes128Encryptor encryptor = new Aes128Encryptor(secretsKeyResolver);
        encryptor.init();

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES128";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        Aes256Encryptor encryptor = new Aes256Encryptor(secretsKeyResolver);
        encryptor.init();

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES256";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAesGcmEncryptAndDecrypt() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(secretsKeyResolver);
        encryptor.init();

        assertTrue(encryptor.isReady());
        String plainText = "Sensitive data for AES-GCM";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(plainText, decrypted);
    }
}
