package org.example.order.core.crypto;

import org.example.order.core.config.OrderCoreConfig;
import org.example.order.core.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration
@SpringBootTest(classes = {OrderCoreConfig.class})
@ActiveProfiles("local")
class EncryptorTest {

    @Autowired
    private EncryptorFactory encryptorFactory;

    @Test
    @DisplayName("AES128 암복호화 테스트 - 팩토리 사용")
    void aes128Test() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AES128);
        assertEncryptorWorks(encryptor);
    }

    @Test
    @DisplayName("AES256 암복호화 테스트 - 팩토리 사용")
    void aes256Test() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AES256);
        assertEncryptorWorks(encryptor);
    }

    @Test
    @DisplayName("AES-GCM 암복호화 테스트 - 팩토리 사용")
    void aesGcmTest() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AESGCM);
        assertEncryptorWorks(encryptor);
    }

    private void assertEncryptorWorks(Encryptor encryptor) {
        assertThat(encryptor.isReady()).isTrue();

        String plainText = "Hello Encryptor";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainText);
    }
}
