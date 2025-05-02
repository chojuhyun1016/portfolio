package org.example.order.core.infra.crypto.factory;

import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.common.helper.encode.Base64Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EncryptorFactoryTest.TestConfig.class
})
class EncryptorFactoryTest {

    @TestConfiguration
    @ComponentScan(basePackages = "org.example.order.core.infra.crypto")
    static class TestConfig {

        @Bean
        public SecretsKeyResolver secretsKeyResolver() {
            SecretsKeyResolver resolver = new SecretsKeyResolver();

            // AES-128: 16바이트 키 스펙
            resolver.updateKey(
                    CryptoAlgorithmType.AES128.name(),
                    createKeySpec("AES-CBC", 128, generateKey(16))
            );

            // AES-256: 32바이트 키 스펙
            resolver.updateKey(
                    CryptoAlgorithmType.AES256.name(),
                    createKeySpec("AES-CBC", 256, generateKey(32))
            );

            // AES-GCM: 32바이트 키 스펙
            resolver.updateKey(
                    CryptoAlgorithmType.AESGCM.name(),
                    createKeySpec("AES-GCM", 256, generateKey(32))
            );

            // HMAC-SHA256: 32바이트 키 스펙 (보통 256bit를 사용)
            resolver.updateKey(
                    CryptoAlgorithmType.HMAC_SHA256.name(),
                    createKeySpec("HMAC-SHA256", 256, generateKey(32))
            );

            return resolver;
        }

        private static CryptoKeySpec createKeySpec(String algorithm, int keySize, byte[] keyBytes) {
            CryptoKeySpec spec = new CryptoKeySpec();
            spec.setAlgorithm(algorithm);
            spec.setKeySize(keySize);
            spec.setValue(Base64Utils.encodeUrlSafe(keyBytes));

            return spec;
        }

        private static byte[] generateKey(int length) {
            byte[] keyBytes = new byte[length];

            for (int i = 0; i < length; i++) {
                keyBytes[i] = (byte) (i + 1);
            }

            return keyBytes;
        }
    }

    @Autowired
    private EncryptorFactory encryptorFactory;

    @Test
    @DisplayName("EncryptorFactory 주입 확인")
    void testFactoryIsLoaded() {
        assertNotNull(encryptorFactory, "EncryptorFactory가 정상 주입되어야 합니다.");
    }

    @Test
    @DisplayName("AES-GCM Encryptor 정상 반환")
    void testGetAesGcmEncryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AESGCM);
        assertNotNull(encryptor, "AES-GCM Encryptor가 null이 아니어야 합니다.");
        assertEquals(CryptoAlgorithmType.AESGCM, encryptor.getType());
    }

    @Test
    @DisplayName("AES-128 Encryptor 정상 반환")
    void testGetAes128Encryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AES128);
        assertNotNull(encryptor, "AES-128 Encryptor가 null이 아니어야 합니다.");
        assertEquals(CryptoAlgorithmType.AES128, encryptor.getType());
    }

    @Test
    @DisplayName("AES-256 Encryptor 정상 반환")
    void testGetAes256Encryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AES256);
        assertNotNull(encryptor, "AES-256 Encryptor가 null이 아니어야 합니다.");
        assertEquals(CryptoAlgorithmType.AES256, encryptor.getType());
    }

    @Test
    @DisplayName("지원하지 않는 Encryptor 타입 예외 처리 확인")
    void testUnsupportedEncryptorThrowsException() {
        CryptoAlgorithmType unsupportedType = CryptoAlgorithmType.ARGON2;
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encryptorFactory.getEncryptor(unsupportedType)
        );

        assertTrue(exception.getMessage().contains("Unsupported encryptor"), "지원하지 않는 타입은 예외를 던져야 합니다.");
    }
}
