package org.example.order.core.crypto.factory;

import org.example.order.core.infra.crypto.Encryptor;
import org.example.order.core.infra.crypto.code.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EncryptorFactoryTest.TestConfig.class)
class EncryptorFactoryTest {

    @TestConfiguration
    @ComponentScan(basePackages = "org.example.order.core.crypto")
    static class TestConfig {

        // 테스트용 암호화 키 생성 (Base64 URL-safe 형식)
        private static String generateKey(int length) {
            byte[] keyBytes = new byte[length];
            for (int i = 0; i < length; i++) keyBytes[i] = (byte) (i + 1);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        }

        @Bean
        public EncryptProperties encryptProperties() {
            EncryptProperties properties = new EncryptProperties();
            properties.getAes128().setKey(generateKey(16));
            properties.getAes256().setKey(generateKey(32));
            properties.getAesgcm().setKey(generateKey(32));

            return properties;
        }
    }

    @Autowired
    private EncryptorFactory encryptorFactory;

    @Test
    @DisplayName("EncryptorFactory 주입 확인")
    void testFactoryIsLoaded() {
        assertNotNull(encryptorFactory, "EncryptorFactory should be injected by Spring");
    }

    @Test
    @DisplayName("AES-GCM Encryptor 정상 반환")
    void testGetAesGcmEncryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AESGCM);
        assertNotNull(encryptor);
        assertEquals(CryptoAlgorithmType.AESGCM, encryptor.getType());
    }

    @Test
    @DisplayName("AES-128 Encryptor 정상 반환")
    void testGetAes128Encryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AES128);
        assertNotNull(encryptor);
        assertEquals(CryptoAlgorithmType.AES128, encryptor.getType());
    }

    @Test
    @DisplayName("AES-256 Encryptor 정상 반환")
    void testGetAes256Encryptor() {
        Encryptor encryptor = encryptorFactory.getEncryptor(CryptoAlgorithmType.AES256);
        assertNotNull(encryptor);
        assertEquals(CryptoAlgorithmType.AES256, encryptor.getType());
    }

    @Test
    @DisplayName("지원하지 않는 Encryptor 타입 예외 처리 확인")
    void testUnsupportedEncryptorThrowsException() {
        CryptoAlgorithmType unsupportedType = CryptoAlgorithmType.HMAC_SHA256; // 예: Encryptor 아님
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encryptorFactory.getEncryptor(unsupportedType)
        );

        assertTrue(exception.getMessage().contains("Unsupported encryptor"));
    }
}
