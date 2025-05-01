package org.example.order.core.infra.crypto.factory.mock;

import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * MockSecretsManagerConfiguration
 *
 * 테스트 환경용으로 SecretsKeyResolver에 미리 키를 주입하는 구성.
 */
@TestConfiguration
public class MockSecretsManagerConfiguration {

    @Bean
    public SecretsKeyResolver secretsKeyResolver() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        // AES-128: 16 bytes key 등록
        resolver.updateKey(
                CryptoAlgorithmType.AES128.name(),
                buildKeySpec("AES-CBC", 128, generateKey(16))
        );

        // AES-256: 32 bytes key 등록
        resolver.updateKey(
                CryptoAlgorithmType.AES256.name(),
                buildKeySpec("AES-CBC", 256, generateKey(32))
        );

        // AES-GCM: 32 bytes key 등록
        resolver.updateKey(
                CryptoAlgorithmType.AESGCM.name(),
                buildKeySpec("AES-GCM", 256, generateKey(32))
        );

        return resolver;
    }

    /**
     * CryptoKeySpec 생성 (Base64로 인코딩하여 세팅)
     */
    private static CryptoKeySpec buildKeySpec(String algorithm, int keySize, byte[] keyBytes) {
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm(algorithm);
        spec.setKeySize(keySize);
        spec.setValue(Base64Utils.encode(keyBytes));

        return spec;
    }

    /**
     * 테스트용 키 생성 (1, 2, 3, ... n)
     */
    private static byte[] generateKey(int length) {
        byte[] keyBytes = new byte[length];

        for (int i = 0; i < length; i++) {
            keyBytes[i] = (byte) (i + 1);
        }

        return keyBytes;
    }
}
