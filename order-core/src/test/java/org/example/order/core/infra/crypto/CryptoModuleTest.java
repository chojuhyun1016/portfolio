package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Crypto 모듈 경량 통합 테스트
 * <p>
 * 전제:
 * - 설정 클래스는 아래 3개만 존재합니다.
 * 1) CryptoManualConfig  : crypto.enabled=true 일 때 알고리즘 빈 + Factory 등록
 * 2) CryptoAutoConfig    : crypto.enabled=true AND crypto.props.seed=true 일 때 encrypt.*.key 자동 시딩
 * 3) EncryptProperties   : encrypt.* 프로퍼티 바인딩(= CryptoManualConfig가 @EnableConfigurationProperties로 등록)
 * <p>
 * 이 테스트는 "메인 클래스 없이" ApplicationContextRunner 로 조건부 자동구성이
 * 의도대로 동작하는지 검증합니다.
 * <p>
 * 포함 시나리오
 * 1) MANUAL 모드: 수동 setKey(...) 후 Encrypt/Decrypt/Hash/Sign 검증
 * 2) AUTO(Seed) 모드: encrypt.*.key 자동 시딩 후 바로 사용 가능
 * 3) Factory 매핑 스모크 테스트
 * 4) OFF 모드(완전 수동): 스프링 없이 new 해서 사용
 * 5) AUTO(Seed) 부분 시딩: 제공된 것만 ready, 나머지는 not ready
 */
class CryptoModuleTest {

    // ---------------------------------------------------------------------
    // 1) MANUAL 모드: crypto.enabled=true 만 켜고, 코드로 setKey(...) 주입해서 사용
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("MANUAL 모드 - setKey 수동 주입 후 알고리즘 동작")
    void manual_mode_setKeys_and_useAlgorithms() {
        // 최소 컨텍스트: Manual 구성만 등록 (properties 바인딩 포함)
        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true" // Manual ON
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class));

        ctx.run(context -> {
            // 빈 등록 확인
            assertThat(context).hasSingleBean(EncryptorFactory.class);
            assertThat(context).hasSingleBean(Aes128Encryptor.class);
            assertThat(context).hasSingleBean(Aes256Encryptor.class);
            assertThat(context).hasSingleBean(AesGcmEncryptor.class);
            assertThat(context).hasSingleBean(HmacSha256Signer.class);

            // Factory 통해 타입별 구현체 획득
            EncryptorFactory factory = context.getBean(EncryptorFactory.class);
            Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
            Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
            Encryptor egcm = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
            Hasher bcrypt = factory.getHasher(CryptoAlgorithmType.BCRYPT);
            Signer hmac = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);

            // 수동 키 주입 (URL-safe Base64, padding 제거)
            e128.setKey(urlSafeKey(16)); // 128-bit
            e256.setKey(urlSafeKey(32)); // 256-bit
            egcm.setKey(urlSafeKey(32)); // 256-bit
            hmac.setKey(urlSafeKey(32)); // 256-bit 공통

            // Encrypt/Decrypt
            String plain = "hello-manual";
            String c1 = e128.encrypt(plain);
            String c2 = e256.encrypt(plain);
            String c3 = egcm.encrypt(plain);

            assertThat(e128.decrypt(c1)).isEqualTo(plain);
            assertThat(e256.decrypt(c2)).isEqualTo(plain);
            assertThat(egcm.decrypt(c3)).isEqualTo(plain);

            // Hash/Verify
            String pw = "p@ssw0rd!";
            String bcryptHash = bcrypt.hash(pw);
            assertThat(bcrypt.matches(pw, bcryptHash)).isTrue();

            // Sign/Verify
            String msg = "sign-manual";
            String sig = hmac.sign(msg);
            assertThat(hmac.verify(msg, sig)).isTrue();
            assertThat(hmac.verify(msg, sig + "x")).isFalse();
        });
    }

    // ---------------------------------------------------------------------
    // 2) AUTO(Seed) 모드: crypto.enabled=true & crypto.props.seed=true
    //    encrypt.*.key 로 자동 시딩 → 즉시 사용 가능
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("AUTO(Seed) 모드 - encrypt.*.key 자동 시딩 후 즉시 사용")
    void auto_seed_mode_properties_injection_and_use() {
        String keyAes128 = urlSafeKey(16);
        String keyAes256 = urlSafeKey(32);
        String keyAesGcm = urlSafeKey(32);
        String keyHmac = urlSafeKey(32);

        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",               // Auto-Seed ON
                        "encrypt.aes128.key=" + keyAes128,
                        "encrypt.aes256.key=" + keyAes256,
                        "encrypt.aesgcm.key=" + keyAesGcm,
                        "encrypt.hmac.key=" + keyHmac
                )
                // Manual → 알고리즘 빈 + Factory
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class))
                // Auto  → seed 실행
                .withConfiguration(UserConfigurations.of(CryptoAutoConfig.class));

        ctx.run(context -> {
            assertThat(context).hasSingleBean(EncryptorFactory.class);

            EncryptorFactory factory = context.getBean(EncryptorFactory.class);
            Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
            Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
            Encryptor egcm = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
            Signer hmac = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);
            Hasher sha = factory.getHasher(CryptoAlgorithmType.SHA256);

            // Auto-Seed 로 모두 ready
            assertThat(e128.isReady()).isTrue();
            assertThat(e256.isReady()).isTrue();
            assertThat(egcm.isReady()).isTrue();
            assertThat(hmac.isReady()).isTrue();

            // Encrypt/Decrypt
            String plain = "hello-auto";
            String c1 = e128.encrypt(plain);
            String c2 = e256.encrypt(plain);
            String c3 = egcm.encrypt(plain);

            assertThat(e128.decrypt(c1)).isEqualTo(plain);
            assertThat(e256.decrypt(c2)).isEqualTo(plain);
            assertThat(egcm.decrypt(c3)).isEqualTo(plain);

            // Sign/Verify
            String msg = "sign-auto";
            String sig = hmac.sign(msg);
            assertThat(hmac.verify(msg, sig)).isTrue();

            // SHA-256 (키 필요 없음)
            String shaHash = sha.hash("abc");
            assertThat(sha.matches("abc", shaHash)).isTrue();
            assertThat(sha.matches("xyz", shaHash)).isFalse();
        });
    }

    // ---------------------------------------------------------------------
    // 3) Factory 매핑 스모크: 타입 → 구현체 정석 바인딩 확인
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Factory 매핑 스모크 - 타입별 구현체 조회 성공")
    void factory_mapping_smoke() {
        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=true")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class));

        ctx.run(context -> {
            EncryptorFactory factory = context.getBean(EncryptorFactory.class);

            assertThat(factory.getEncryptor(CryptoAlgorithmType.AES128)).isNotNull();
            assertThat(factory.getEncryptor(CryptoAlgorithmType.AES256)).isNotNull();
            assertThat(factory.getEncryptor(CryptoAlgorithmType.AESGCM)).isNotNull();

            assertThat(factory.getHasher(CryptoAlgorithmType.BCRYPT)).isNotNull();
            assertThat(factory.getHasher(CryptoAlgorithmType.ARGON2)).isNotNull();
            assertThat(factory.getHasher(CryptoAlgorithmType.SHA256)).isNotNull();

            assertThat(factory.getSigner(CryptoAlgorithmType.HMAC_SHA256)).isNotNull();
        });
    }

    // ---------------------------------------------------------------------
    // 4) OFF 모드(완전 수동): 스프링 없이 new 해서 사용하는 순수 수동 시나리오
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("OFF 모드(완전 수동) - 스프링 없이 new/setKey/사용")
    void off_mode_pure_manual_new_and_use() {
        // 스프링 컨텍스트 없이 직접 생성
        Aes128Encryptor e128 = new Aes128Encryptor();
        Aes256Encryptor e256 = new Aes256Encryptor();
        AesGcmEncryptor egcm = new AesGcmEncryptor();
        HmacSha256Signer hmac = new HmacSha256Signer();

        // 수동 키 주입
        e128.setKey(urlSafeKey(16));
        e256.setKey(urlSafeKey(32));
        egcm.setKey(urlSafeKey(32));
        hmac.setKey(urlSafeKey(32));

        // 동작 확인
        String plain = "hello-off";
        String c1 = e128.encrypt(plain);
        String c2 = e256.encrypt(plain);
        String c3 = egcm.encrypt(plain);

        assertThat(e128.decrypt(c1)).isEqualTo(plain);
        assertThat(e256.decrypt(c2)).isEqualTo(plain);
        assertThat(egcm.decrypt(c3)).isEqualTo(plain);

        String msg = "sign-off";
        String sig = hmac.sign(msg);
        assertThat(hmac.verify(msg, sig)).isTrue();
    }

    // ---------------------------------------------------------------------
    // 5) AUTO(Seed) 부분 시딩: 일부만 키 제공 → 해당 알고리즘만 ready
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("AUTO(Seed) 부분 시딩 - 제공된 알고리즘만 ready")
    void auto_seed_partial_only_provided_are_ready() {
        String keyAes256 = urlSafeKey(32); // AES-256만 시딩

        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",
                        "encrypt.aes256.key=" + keyAes256
                        // aes128/aesgcm/hmac 키는 의도적으로 미제공
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class))
                .withConfiguration(UserConfigurations.of(CryptoAutoConfig.class));

        ctx.run(context -> {
            EncryptorFactory factory = context.getBean(EncryptorFactory.class);

            Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
            Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
            Encryptor egcm = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
            Signer hmac = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);

            assertThat(e256.isReady()).isTrue();   // 제공됨
            assertThat(e128.isReady()).isFalse();  // 미제공
            assertThat(egcm.isReady()).isFalse();  // 미제공
            assertThat(hmac.isReady()).isFalse();  // 미제공
        });
    }

    // ===== 테스트 유틸 =====

    /**
     * URL-safe Base64(무패딩) 랜덤 키 생성
     *
     * @param bytes 키 바이트 길이 (AES128=16, AES256/GCM/HMAC=32)
     */
    private static String urlSafeKey(int bytes) {
        byte[] buf = new byte[bytes];
        new java.security.SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
