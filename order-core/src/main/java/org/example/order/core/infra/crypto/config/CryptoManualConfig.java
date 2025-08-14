package org.example.order.core.infra.crypto.config;

import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.algorithm.hasher.Argon2Hasher;
import org.example.order.core.infra.crypto.algorithm.hasher.BcryptHasher;
import org.example.order.core.infra.crypto.algorithm.hasher.Sha256Hasher;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Crypto 코어 자동구성 (기본 OFF)
 * - crypto.enabled=true 일 때만 암호 알고리즘 빈들이 등록됩니다.
 * - 각 구현체는 @Component를 사용하지 않고, 이 구성에서 @Bean으로만 노출합니다.
 * - 키는 설정/코드로 별도 주입(옵션) - 미주입 상태에서 사용 시 IllegalStateException 가드 유지.
 */
@Configuration
@EnableConfigurationProperties(EncryptProperties.class)
@ConditionalOnProperty(name = "crypto.enabled", havingValue = "true")
public class CryptoManualConfig {

    /* ========= Encryptors ========= */

    @Bean
    @ConditionalOnMissingBean
    public Aes128Encryptor aes128Encryptor() {
        return new Aes128Encryptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public Aes256Encryptor aes256Encryptor() {
        return new Aes256Encryptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AesGcmEncryptor aesGcmEncryptor() {
        return new AesGcmEncryptor();
    }

    /* ========= Hashers ========= */

    @Bean
    @ConditionalOnMissingBean
    public BcryptHasher bcryptHasher() {
        return new BcryptHasher();
    }

    @Bean
    @ConditionalOnMissingBean
    public Argon2Hasher argon2Hasher() {
        return new Argon2Hasher();
    }

    @Bean
    @ConditionalOnMissingBean
    public Sha256Hasher sha256Hasher() {
        return new Sha256Hasher();
    }

    /* ========= Signers ========= */

    @Bean
    @ConditionalOnMissingBean
    public HmacSha256Signer hmacSha256Signer() {
        return new HmacSha256Signer();
    }

    /* ========= Factory ========= */

    /**
     * 알고리즘 팩토리
     * - Factory 역시 @Component 미사용. 여기서만 빈으로 노출.
     */
    @Bean
    @ConditionalOnMissingBean
    public EncryptorFactory encryptorFactory(
            List<Encryptor> encryptors,
            List<Hasher> hashers,
            List<Signer> signers
    ) {
        return new EncryptorFactory(encryptors, hashers, signers);
    }
}
