package org.example.order.core.crypto;

import org.example.order.core.crypto.code.EncryptorType;
import org.example.order.core.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EncryptorTest.TestConfig.class)
@ActiveProfiles("local")
class EncryptorTest {

    @Autowired
    private EncryptorFactory encryptorFactory;

    @Test
    @DisplayName("AES128 암복호화 테스트 - 팩토리 사용")
    void aes128Test() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AES128);
        assertEncryptorWorks(encryptor, EncryptorType.AES128);
    }

    @Test
    @DisplayName("AES256 암복호화 테스트 - 팩토리 사용")
    void aes256Test() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AES256);
        assertEncryptorWorks(encryptor, EncryptorType.AES256);
    }

    @Test
    @DisplayName("AES-GCM 암복호화 테스트 - 팩토리 사용")
    void aesGcmTest() {
        Encryptor encryptor = encryptorFactory.getEncryptor(EncryptorType.AESGCM);
        assertEncryptorWorks(encryptor, EncryptorType.AESGCM);
    }

    private void assertEncryptorWorks(Encryptor encryptor, EncryptorType type) {
        assertThat(encryptor.isReady()).isTrue();

        String plainText = "Hello Encryptor";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);

        // 콘솔 출력
        System.out.println("[" + type + "] 암복호화 테스트 결과");
        System.out.println("원문(Plain)    : " + plainText);
        System.out.println("암호문(Encrypted): " + encrypted);
        System.out.println("복호문(Decrypted): " + decrypted);
        System.out.println();

        assertThat(decrypted).isEqualTo(plainText);
    }

    /**
     * 테스트용 Configuration 클래스
     * Redis / JPA / DataSource 자동설정 제외
     */
    @Configuration
    @EnableAutoConfiguration(exclude = {
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @ComponentScan(basePackages = {
            "org.example.order.core.crypto",
            "org.example.order.core.crypto.factory"
    })
    public static class TestConfig {
    }
}
