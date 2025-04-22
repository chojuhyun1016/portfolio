package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignerTest {

    @Test
    void testHmacSha256SignAndVerify() {
        // 테스트용 키 생성 (Base64 URL-safe encoded)
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.HMAC_SHA256);
        EncryptProperties properties = new EncryptProperties();
        properties.getHmac().setKey(base64Key);

        // URL-safe base64 decode 가능하도록 수정된 모의 KMS
        KmsDecryptor mockKmsDecryptor = new KmsDecryptor(properties) {
            @Override
            public byte[] decryptBase64EncodedKey(String base64Key) {
                return java.util.Base64.getUrlDecoder().decode(base64Key);
            }
        };

        // Signer 생성 및 수동 초기화
        HmacSha256Signer signer = new HmacSha256Signer(properties, mockKmsDecryptor);
        signer.init();

        assertTrue(signer.isReady());
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, signer.getType());

        String message = "important-message";
        String signature = signer.sign(message);

        assertNotNull(signature);
        assertTrue(signer.verify(message, signature));
        assertFalse(signer.verify("tampered-message", signature));
    }
}
