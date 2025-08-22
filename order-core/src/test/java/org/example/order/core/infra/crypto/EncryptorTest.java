package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Encryptor 단위 기능 테스트 (시딩 필수)
 */
class EncryptorTest {

    // ✅ CryptoAutoConfig / 각 Encryptor는 URL-safe Base64 키를 기대한다.
    // 기존 Base64.getEncoder() 대신 URL-safe + no padding 으로 생성해야 한다.
    private static String b64UrlKey(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private ApplicationContextRunner runnerSeeded() {
        return new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",
                        // AES-128(16B), AES-256(32B) URL-safe Base64 키로 시딩
                        "encrypt.aes128.key=" + b64UrlKey(16),
                        "encrypt.aes256.key=" + b64UrlKey(32),
                        // ⚠️ AES-GCM 구현은 32바이트 키(256-bit)만 허용하므로 32B로 시딩
                        "encrypt.aesgcm.key=" + b64UrlKey(32),
                        // HMAC 키도 URL-safe Base64 로 맞춘다 (Signer 테스트와 동일 원칙)
                        "encrypt.hmac.key=" + b64UrlKey(32)
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class));
    }

    @Test
    void aes128_roundtrip() {
        runnerSeeded().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Encryptor e = f.getEncryptor(CryptoAlgorithmType.AES128);
            String ct = e.encrypt("plain-128");
            assertThat(e.decrypt(ct)).isEqualTo("plain-128");
        });
    }

    @Test
    void aes256_roundtrip() {
        runnerSeeded().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Encryptor e = f.getEncryptor(CryptoAlgorithmType.AES256);
            String ct = e.encrypt("plain-256");
            assertThat(e.decrypt(ct)).isEqualTo("plain-256");
        });
    }

    @Test
    void aesgcm_roundtrip() {
        runnerSeeded().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Encryptor e = f.getEncryptor(CryptoAlgorithmType.AESGCM);
            String ct = e.encrypt("plain-gcm");
            assertThat(e.decrypt(ct)).isEqualTo("plain-gcm");
        });
    }
}
