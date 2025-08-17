package org.example.order.core.infra.crypto.config;

import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.props.EncryptProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crypto 키 시딩 구성 (옵션)
 * - crypto.enabled=true AND crypto.props.seed=true 일 때만 활성화
 * - application.yml의 encrypt.*.key 값을 해당 알고리즘 빈에 setKey(...)로 주입
 * - 값이 비어있으면 건너뜀 (부분 시딩 허용)
 */
@Configuration
@ConditionalOnProperty(name = {"crypto.enabled", "crypto.props.seed"}, havingValue = "true")
@ConditionalOnBean(CryptoManualConfig.class)
public class CryptoAutoConfig {

    @Bean
    public InitializingBean cryptoKeysSeed(
            EncryptProperties props,
            Aes128Encryptor aes128Encryptor,
            Aes256Encryptor aes256Encryptor,
            AesGcmEncryptor aesGcmEncryptor,
            HmacSha256Signer hmacSha256Signer
    ) {
        return () -> {
            if (props.getAes128() != null && notBlank(props.getAes128().getKey())) {
                aes128Encryptor.setKey(props.getAes128().getKey());
            }
            if (props.getAes256() != null && notBlank(props.getAes256().getKey())) {
                aes256Encryptor.setKey(props.getAes256().getKey());
            }
            if (props.getAesgcm() != null && notBlank(props.getAesgcm().getKey())) {
                aesGcmEncryptor.setKey(props.getAesgcm().getKey());
            }
            if (props.getHmac() != null && notBlank(props.getHmac().getKey())) {
                hmacSha256Signer.setKey(props.getHmac().getKey());
            }
        };
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
