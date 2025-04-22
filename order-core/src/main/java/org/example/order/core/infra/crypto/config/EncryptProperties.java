package org.example.order.core.infra.crypto.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "encrypt")
public class EncryptProperties {

    private String kmsRegion;

    private Aes128 aes128 = new Aes128();
    private Aes256 aes256 = new Aes256();
    private AesGcm aesgcm = new AesGcm();
    private Hmac hmac = new Hmac();

    @Getter
    @Setter
    public static class Aes128 {
        private String key;
    }

    @Getter
    @Setter
    public static class Aes256 {
        private String key;
    }

    @Getter
    @Setter
    public static class AesGcm {
        private String key;
    }

    @Getter
    @Setter
    public static class Hmac {
        private String key;
    }
}
