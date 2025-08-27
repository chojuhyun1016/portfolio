package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Signer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SignerTest {

    private static String b64UrlKey(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    @Test
    void hmac_sha256_sign_and_verify() {
        String khmac = b64UrlKey(32); // ✅ URL-Safe Base64

        new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",
                        "encrypt.hmac.key=" + khmac
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class))
                .run(ctx -> {
                    var factory = ctx.getBean(org.example.order.core.infra.crypto.factory.EncryptorFactory.class);
                    Signer signer = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);

                    String msg = "sign-me";
                    String sig = signer.sign(msg);

                    assertThat(sig).isNotBlank();
                    assertThat(signer.verify(msg, sig)).isTrue();
                    assertThat(signer.verify(msg + "x", sig)).isFalse();
                });
    }
}
