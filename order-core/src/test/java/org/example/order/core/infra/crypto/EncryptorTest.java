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

class EncryptorTest {

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
                        "encrypt.aes128.key=" + b64UrlKey(16),
                        "encrypt.aes256.key=" + b64UrlKey(32),
                        "encrypt.aesgcm.key=" + b64UrlKey(32),
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
