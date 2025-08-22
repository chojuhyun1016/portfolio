package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hasher 단위 기능 테스트
 * - BCRYPT / ARGON2 / SHA256
 * - 키 시딩 불필요 (해시류는 대개 키가 필요 없음)
 */
class HasherTest {

    private ApplicationContextRunner runnerEnabled() {
        return new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=true")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class));
    }

    @Test
    void bcrypt_matches() {
        runnerEnabled().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Hasher h = f.getHasher(CryptoAlgorithmType.BCRYPT);
            String hash = h.hash("p@ss");
            assertThat(h.matches("p@ss", hash)).isTrue();
        });
    }

    @Test
    void argon2_matches() {
        runnerEnabled().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Hasher h = f.getHasher(CryptoAlgorithmType.ARGON2);
            String hash = h.hash("p@ss");
            assertThat(h.matches("p@ss", hash)).isTrue();
        });
    }

    @Test
    void sha256_hash_not_empty() {
        runnerEnabled().run(ctx -> {
            EncryptorFactory f = ctx.getBean(EncryptorFactory.class);
            Hasher h = f.getHasher(CryptoAlgorithmType.SHA256);
            String hash = h.hash("abc");
            assertThat(hash).isNotBlank();
        });
    }
}
