package org.example.order.core.infra.crypto;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HmacSha256Signer 단위 테스트 - 키매니저1 기준
 */
class SignerTest {

    private SecretsKeyResolver secretsKeyResolver;

    @BeforeEach
    void setup() {
        // SecretsKeyResolver 준비
        this.secretsKeyResolver = new SecretsKeyResolver();

        // 테스트용 HMAC 키 생성 및 등록 (32 bytes: HMAC-SHA256 권장 크기)
        byte[] hmacKey = generateKey(32);

        // CryptoKeySpec 생성 후 등록
        CryptoKeySpec hmacKeySpec = new CryptoKeySpec();
        hmacKeySpec.setAlgorithm("HMAC-SHA256");
        hmacKeySpec.setKeySize(256);
        hmacKeySpec.setValue(Base64Utils.encodeUrlSafe(hmacKey));

        // SecretsKeyResolver에 HMAC 키 등록
        secretsKeyResolver.updateKey(CryptoAlgorithmType.HMAC_SHA256.name(), hmacKeySpec);
    }

    private byte[] generateKey(int length) {
        byte[] keyBytes = new byte[length];

        for (int i = 0; i < length; i++) {
            keyBytes[i] = (byte) (i + 1);  // 간단한 테스트용 키 생성
        }

        return keyBytes;
    }

    @Test
    void testHmacSha256SignAndVerify() {
        // Signer 생성 및 초기화
        HmacSha256Signer signer = new HmacSha256Signer(secretsKeyResolver);
        signer.init();

        // Signer 기능 검증
        assertTrue(signer.isReady());
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, signer.getType());

        String message = "important-message";
        String signature = signer.sign(message);

        assertNotNull(signature);
        assertTrue(signer.verify(message, signature));

        // 변조된 메시지는 검증 실패
        assertFalse(signer.verify("tampered-message", signature));
    }
}
