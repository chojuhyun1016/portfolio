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
import org.example.order.core.infra.crypto.props.EncryptProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

/**
 * Crypto 인프라 통합 구성 (단일 진입점)
 * <p>
 * - kafka/s3/web/tsid/secrets 과 동일한 패턴
 * - crypto.enabled=true 일 때만 전체 블록 활성화
 * - Core Bean(Encryptors, Hashers, Signer, Factory) 등록
 * - crypto.props.seed=true 이면 설정값(encrypt.*.key)을 각 구현체에 시딩
 * <p>
 * 사용법:
 *
 * @Import(CryptoInfraConfig.class)
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EncryptProperties.class)
@ConditionalOnProperty(prefix = "crypto", name = "enabled", havingValue = "true")
@Import(CryptoInfraConfig.SeedConfig.class)
public class CryptoInfraConfig {

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
     * - @Component 미사용, 설정 기반 @Bean으로만 노출
     */
    @Bean
    @ConditionalOnMissingBean
    public EncryptorFactory encryptorFactory(java.util.List<Encryptor> encryptors, java.util.List<Hasher> hashers, java.util.List<Signer> signers) {
        return new EncryptorFactory(encryptors, hashers, signers);
    }

    /**
     * 시딩 서브 구성
     * - crypto.props.seed=true 일 때만 활성화
     * - encrypt.*.key 가 비어있지 않으면 해당 구현체에 setKey(...) 주입
     * - 구현체 빈이 커스터마이즈되어 없을 수 있으므로 ObjectProvider로 안전 주입
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "crypto.props", name = "seed", havingValue = "true")
    public static class SeedConfig {

        @Bean
        public InitializingBean cryptoKeysSeed(EncryptProperties props, ObjectProvider<Aes128Encryptor> aes128Provider, ObjectProvider<Aes256Encryptor> aes256Provider, ObjectProvider<AesGcmEncryptor> aesgcmProvider, ObjectProvider<HmacSha256Signer> hmacProvider) {
            return () -> {
                var aes128 = aes128Provider.getIfAvailable();
                var aes256 = aes256Provider.getIfAvailable();
                var aesgcm = aesgcmProvider.getIfAvailable();
                var hmac = hmacProvider.getIfAvailable();

                if (props.getAes128() != null && notBlank(props.getAes128().getKey()) && aes128 != null) {
                    aes128.setKey(props.getAes128().getKey());
                }

                if (props.getAes256() != null && notBlank(props.getAes256().getKey()) && aes256 != null) {
                    aes256.setKey(props.getAes256().getKey());
                }

                if (props.getAesgcm() != null && notBlank(props.getAesgcm().getKey()) && aesgcm != null) {
                    aesgcm.setKey(props.getAesgcm().getKey());
                }

                if (props.getHmac() != null && notBlank(props.getHmac().getKey()) && hmac != null) {
                    hmac.setKey(props.getHmac().getKey());
                }
            };
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }
}
