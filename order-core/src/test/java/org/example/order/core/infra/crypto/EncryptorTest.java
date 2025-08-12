package org.example.order.core.infra.crypto;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Encryptor 계열 (AES128 / AES256 / AES-GCM) 단위 테스트 – SecretsKeyResolver 제거 버전
 * <p>
 * - 각 Encryptor 인스턴스를 직접 생성하고 setKey()로 키 주입
 * - 암복호화 왕복 검증
 */
class EncryptorTest {

    private static final SecureRandom RND = new SecureRandom();

    private static String b64Key(int bytes) {
        byte[] k = new byte[bytes];
        RND.nextBytes(k);
        return Base64Utils.encodeUrlSafe(k);
    }

    @Test
    void testAes128EncryptAndDecrypt() {
        Aes128Encryptor encryptor = new Aes128Encryptor();
        encryptor.setKey(b64Key(16)); // 128-bit

        assertTrue(encryptor.isReady());
        String plain = "Sensitive data for AES128";
        String enc = encryptor.encrypt(plain);
        String dec = encryptor.decrypt(enc);

        assertNotNull(enc);
        assertEquals(plain, dec);
    }

    @Test
    void testAes256EncryptAndDecrypt() {
        Aes256Encryptor encryptor = new Aes256Encryptor();
        encryptor.setKey(b64Key(32)); // 256-bit

        assertTrue(encryptor.isReady());
        String plain = "Sensitive data for AES256";
        String enc = encryptor.encrypt(plain);
        String dec = encryptor.decrypt(enc);

        assertNotNull(enc);
        assertEquals(plain, dec);
    }

    @Test
    void testAesGcmEncryptAndDecrypt() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor();
        encryptor.setKey(b64Key(32)); // 256-bit

        assertTrue(encryptor.isReady());
        String plain = "Sensitive data for AES-GCM";
        String enc = encryptor.encrypt(plain);
        String dec = encryptor.decrypt(enc);

        assertNotNull(enc);
        assertEquals(plain, dec);
    }
}
