package org.example.order.core.crypto.factory;

import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = EncryptorFactoryTest.TestConfig.class)
class EncryptorFactoryTest {

    @Configuration
    @ComponentScan(basePackages = "org.example.order.core.crypto")
    static class TestConfig {
        // 필요한 경우 Bean 직접 등록 가능
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
