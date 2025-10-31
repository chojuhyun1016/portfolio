package org.example.order.worker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AppCryptoKeyProperties
 * ------------------------------------------------------------------------
 * 목적
 * - 컬럼/도메인별로 서로 다른 암호화 알고리즘·키를 동시에 운용하기 위한 설정.
 * - 각 엔트리: alias(Secrets의 키 묶음 이름), encryptor(AES128/AES256/AESGCM), kid 또는 version으로 “핀”.
 * <p>
 * [예시(yaml)]
 * app:
 * crypto:
 * keys:
 * orderAesGcm:
 * alias: "order.aesgcm"
 * encryptor: "AESGCM"
 * kid: "key-2025-09-27"
 * userPhoneAes256:
 * alias: "user.phone.aes256"
 * encryptor: "AES256"
 * version: 2
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.crypto")
public class AppCryptoKeyProperties {

    /**
     * keys:
     * <logical-name>:
     * alias: "order.aesgcm"
     * encryptor: "AESGCM" | "AES256" | "AES128"
     * kid: "key-..."        # 선택 1 (우선)
     * version: 1            # 선택 2 (kid 없을 때)
     */
    private Map<String, Alias> keys = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Alias {
        private String alias;      // Secrets alias
        private String encryptor;  // "AES128" | "AES256" | "AESGCM"
        private String kid;        // 우선(지정되면 version보다 우위)
        private Integer version;   // kid가 없을 때만 사용
    }
}
