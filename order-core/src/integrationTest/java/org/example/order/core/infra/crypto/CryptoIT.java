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

// ✅ 변경 포인트: 테스트 전용 최소 부트 컨텍스트 + 자동설정 제외를 위해 필요한 import
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

import jakarta.annotation.Resource; // ✅ Spring Boot 3.x에서는 jakarta 패키지 사용

/**
 * 통합 테스트:
 *
 * ✅ 변경 요약
 * - 이전: @SpringBootTest 로 IntegrationBootApp 경유 → infra.redis 유입 → Redisson 자동설정 시도
 * - 현재: 테스트 내부 Boot 컨텍스트(CryptoIT.Boot) + @ImportAutoConfiguration(exclude=…) 로
 *         Redisson/Redis 자동구성만 테스트 컨텍스트에서 제외하여 Redis 연결 실패 제거.
 * - Crypto 기능 자체는 수동/자동 설정 클래스를 @Import 로 정확히 로딩.
 */
@SpringBootTest(classes = CryptoIT.Boot.class) // ✅ 최소 컨텍스트 사용
@Import({CryptoManualConfig.class, CryptoAutoConfig.class})
@ImportAutoConfiguration(exclude = {
        // ✅ 이 테스트는 Redis/Redisson과 무관 → 자동설정 제외
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoIT {

    /**
     * ✅ 테스트 전용 최소 부트 컨텍스트
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot { }

    private static String b64Key(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        // ✅ 운영 코드가 URL-safe 디코더를 쓰므로, 테스트 키도 URL-safe로 인코딩
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("crypto.enabled", () -> "true");
        r.add("crypto.props.seed", () -> "true");
        r.add("encrypt.aes128.key", () -> b64Key(16));
        r.add("encrypt.aes256.key", () -> b64Key(32));
        r.add("encrypt.aesgcm.key", () -> b64Key(32)); // ⚠️ AES-GCM은 32바이트 키 적용
        r.add("encrypt.hmac.key", () -> b64Key(32));
    }

    @Resource
    private EncryptorFactory factory;

    @Test
    void end_to_end_encrypt_hash_sign() {
        // Encrypt/Decrypt
        Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
        String c1 = e128.encrypt("it-128");
        assertThat(e128.decrypt(c1)).isEqualTo("it-128");

        Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
        String c2 = e256.encrypt("it-256");
        assertThat(e256.decrypt(c2)).isEqualTo("it-256");

        Encryptor egcm = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
        String c3 = egcm.encrypt("it-gcm");
        assertThat(egcm.decrypt(c3)).isEqualTo("it-gcm");

        // Sign/Verify
        Signer signer = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);
        String msg = "hello-integration";
        String sig = signer.sign(msg);
        assertThat(signer.verify(msg, sig)).isTrue();

        // Hash
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
