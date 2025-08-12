package org.example.order.core.infra.crypto;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HmacSha256Signer 단위 테스트 – SecretsKeyResolver 제거 버전
 * <p>
 * - Signer 인스턴스를 직접 생성하고 setKey()로 키 주입
 * - 서명/검증 왕복 검증
 */
class SignerTest {

    private static final SecureRandom RND = new SecureRandom();

    private static String b64Key(int bytes) {
        byte[] k = new byte[bytes];
        RND.nextBytes(k);
        return Base64Utils.encodeUrlSafe(k);
    }

    @Test
    void testHmacSha256SignAndVerify() {
        HmacSha256Signer signer = new HmacSha256Signer();
        signer.setKey(b64Key(32)); // 256-bit 권장

        assertTrue(signer.isReady());
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, signer.getType());

        String message = "important-message";
        String sig = signer.sign(message);

        assertNotNull(sig);
        assertTrue(signer.verify(message, sig));
        assertFalse(signer.verify("tampered-message", sig));
    }
}
