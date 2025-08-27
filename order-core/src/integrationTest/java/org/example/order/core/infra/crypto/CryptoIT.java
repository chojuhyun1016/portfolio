package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.Resource;

@SpringBootTest(classes = CryptoIT.Boot.class)
@Import({CryptoManualConfig.class, CryptoAutoConfig.class})
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot {
    }

    private static String b64Key(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("crypto.enabled", () -> "true");
        r.add("crypto.props.seed", () -> "true");
        r.add("encrypt.aes128.key", () -> b64Key(16));
        r.add("encrypt.aes256.key", () -> b64Key(32));
        r.add("encrypt.aesgcm.key", () -> b64Key(32));
        r.add("encrypt.hmac.key", () -> b64Key(32));
    }

    @Resource
    private EncryptorFactory factory;

    @Test
    void end_to_end_encrypt_hash_sign() {
        Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
        String c1 = e128.encrypt("it-128");
        assertThat(e128.decrypt(c1)).isEqualTo("it-128");

        Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
        String c2 = e256.encrypt("it-256");
        assertThat(e256.decrypt(c2)).isEqualTo("it-256");

        Encryptor egcm = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
        String c3 = egcm.encrypt("it-gcm");
        assertThat(egcm.decrypt(c3)).isEqualTo("it-gcm");

        Signer signer = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);
        String msg = "hello-integration";
        String sig = signer.sign(msg);
        assertThat(signer.verify(msg, sig)).isTrue();

        Hasher bcrypt = factory.getHasher(CryptoAlgorithmType.BCRYPT);
        String bh = bcrypt.hash("pw!");
        assertThat(bcrypt.matches("pw!", bh)).isTrue();

        Hasher argon2 = factory.getHasher(CryptoAlgorithmType.ARGON2);
        String ah = argon2.hash("pw!");
        assertThat(argon2.matches("pw!", ah)).isTrue();

        Hasher sha256 = factory.getHasher(CryptoAlgorithmType.SHA256);
        String sh = sha256.hash("x");
        assertThat(sh).isNotEmpty();
    }
}
