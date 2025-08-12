package org.example.order.core.infra.crypto.factory;

import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.algorithm.hasher.Argon2Hasher;
import org.example.order.core.infra.crypto.algorithm.hasher.BcryptHasher;
import org.example.order.core.infra.crypto.algorithm.hasher.Sha256Hasher;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.common.helper.encode.Base64Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptorFactory 테스트 (SecretsKeyResolver 제거 버전)
 * <p>
 * - Encryptor/Signer 인스턴스를 직접 생성하고 setKey()로 키 주입
 * - Factory 생성 시 리스트로 전달하여 타입별 조회 검증
 */
class EncryptorFactoryTest {

    private static final SecureRandom RND = new SecureRandom();

    private static String b64Key(int bytes) {
        byte[] k = new byte[bytes];
        RND.nextBytes(k);
        return Base64Utils.encodeUrlSafe(k);
    }

    @Test
    @DisplayName("EncryptorFactory 주입/조회 정상 동작")
    void testFactoryLookup() {
        // Encryptors (키 사전 주입)
        Aes128Encryptor aes128 = new Aes128Encryptor();
        aes128.setKey(b64Key(16));

        Aes256Encryptor aes256 = new Aes256Encryptor();
        aes256.setKey(b64Key(32));

        AesGcmEncryptor aesgcm = new AesGcmEncryptor();
        aesgcm.setKey(b64Key(32));

        // Hashers
        Hasher bcrypt = new BcryptHasher();
        Hasher argon2 = new Argon2Hasher();
        Hasher sha256 = new Sha256Hasher();

        // Signer (키 사전 주입)
        HmacSha256Signer hmac = new HmacSha256Signer();
        hmac.setKey(b64Key(32));

        EncryptorFactory factory = new EncryptorFactory(
                List.of(aes128, aes256, aesgcm),
                List.of(bcrypt, argon2, sha256),
                List.of(hmac)
        );

        // Encryptor 조회
        Encryptor g1 = factory.getEncryptor(CryptoAlgorithmType.AESGCM);
        assertNotNull(g1);
        assertEquals(CryptoAlgorithmType.AESGCM, g1.getType());

        Encryptor e128 = factory.getEncryptor(CryptoAlgorithmType.AES128);
        assertNotNull(e128);
        assertEquals(CryptoAlgorithmType.AES128, e128.getType());

        Encryptor e256 = factory.getEncryptor(CryptoAlgorithmType.AES256);
        assertNotNull(e256);
        assertEquals(CryptoAlgorithmType.AES256, e256.getType());

        // Signer 조회
        Signer s = factory.getSigner(CryptoAlgorithmType.HMAC_SHA256);
        assertNotNull(s);
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, s.getType());
    }

    @Test
    @DisplayName("지원하지 않는 Encryptor 타입 예외 처리 확인")
    void testUnsupportedEncryptorThrowsException() {
        EncryptorFactory factory = new EncryptorFactory(
                List.of(new Aes128Encryptor()), // 키 미주입해도 조회만 검증
                List.of(new BcryptHasher()),
                List.of(new HmacSha256Signer())
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getEncryptor(CryptoAlgorithmType.ARGON2) // Encryptor 아님
        );
        assertTrue(ex.getMessage().contains("Unsupported encryptor"));
    }
}
