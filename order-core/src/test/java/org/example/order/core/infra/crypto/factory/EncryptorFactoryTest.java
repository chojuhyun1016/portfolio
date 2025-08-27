package org.example.order.core.infra.crypto.factory;

import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.contract.Signer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EncryptorFactoryTest {

    private static String b64UrlKey(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    @Test
    void when_disabled_then_no_beans() {
        new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=false")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(EncryptorFactory.class));
    }

    @Test
    void when_enabled_without_seed_then_factory_and_algorithms_exist_but_no_usage() {
        new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=true")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(EncryptorFactory.class);

                    EncryptorFactory f = ctx.getBean(EncryptorFactory.class);

                    assertThat(f.getEncryptor(CryptoAlgorithmType.AES128)).isNotNull();
                    assertThat(f.getEncryptor(CryptoAlgorithmType.AES256)).isNotNull();
                    assertThat(f.getEncryptor(CryptoAlgorithmType.AESGCM)).isNotNull();
                    assertThat(f.getSigner(CryptoAlgorithmType.HMAC_SHA256)).isNotNull();
                    assertThat(f.getHasher(CryptoAlgorithmType.BCRYPT)).isNotNull();
                    assertThat(f.getHasher(CryptoAlgorithmType.ARGON2)).isNotNull();
                    assertThat(f.getHasher(CryptoAlgorithmType.SHA256)).isNotNull();
                });
    }

    @Test
    void when_enabled_and_seeded_then_end_to_end_sanity_via_factory() {
        String k128 = b64UrlKey(16);
        String k256 = b64UrlKey(32);
        String kgcm = b64UrlKey(32);
        String khmac = b64UrlKey(32);

        new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",
                        "encrypt.aes128.key=" + k128,
                        "encrypt.aes256.key=" + k256,
                        "encrypt.aesgcm.key=" + kgcm,
                        "encrypt.hmac.key=" + khmac
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class))
                .run(ctx -> {
                    EncryptorFactory f = ctx.getBean(EncryptorFactory.class);

                    Encryptor e128 = f.getEncryptor(CryptoAlgorithmType.AES128);
                    String c1 = e128.encrypt("hello");
                    assertThat(e128.decrypt(c1)).isEqualTo("hello");

                    Encryptor e256 = f.getEncryptor(CryptoAlgorithmType.AES256);
                    String c2 = e256.encrypt("world");
                    assertThat(e256.decrypt(c2)).isEqualTo("world");

                    Encryptor egcm = f.getEncryptor(CryptoAlgorithmType.AESGCM);
                    String c3 = egcm.encrypt("gcm");
                    assertThat(egcm.decrypt(c3)).isEqualTo("gcm");

                    Signer signer = f.getSigner(CryptoAlgorithmType.HMAC_SHA256);
                    String sig = signer.sign("data");
                    assertThat(signer.verify("data", sig)).isTrue();

                    Hasher bcrypt = f.getHasher(CryptoAlgorithmType.BCRYPT);
                    String bh = bcrypt.hash("pw");
                    assertThat(bcrypt.matches("pw", bh)).isTrue();

                    Hasher argon2 = f.getHasher(CryptoAlgorithmType.ARGON2);
                    String ah = argon2.hash("pw");
                    assertThat(argon2.matches("pw", ah)).isTrue();

                    Hasher sha256 = f.getHasher(CryptoAlgorithmType.SHA256);
                    String sh = sha256.hash("x");
                    assertThat(sh).isNotEmpty();

                    assertThatCode(() -> sha256.matches("x", sh)).doesNotThrowAnyException();
                });
    }
}
