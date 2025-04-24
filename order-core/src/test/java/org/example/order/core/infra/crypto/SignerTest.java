package org.example.order.core.infra.crypto;

import org.example.order.core.infra.common.kms.config.KmsProperties;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SignerTest {

    @Test
    void testHmacSha256SignAndVerify() {
        // 1. 테스트용 HMAC 키 생성 (Base64 URL-safe encoded)
        String base64Key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.HMAC_SHA256);

        // 2. EncryptProperties 생성 후 HMAC 키 설정
        EncryptProperties encryptProperties = new EncryptProperties();
        EncryptProperties.Hmac hmac = new EncryptProperties.Hmac();
        hmac.setKey(base64Key);
        encryptProperties.setHmac(hmac);

        // 3. base64 디코딩만 수행하는 mock KMSDecryptor 정의 (명시적 생성자 필요)
        KmsDecryptor mockKmsDecryptor = new KmsDecryptor(new KmsProperties()) {
            @Override
            public byte[] decryptBase64EncodedKey(String base64Key) {
                return Base64.getUrlDecoder().decode(base64Key);
            }
        };

        // 4. Signer 생성 및 초기화
        HmacSha256Signer signer = new HmacSha256Signer(encryptProperties, mockKmsDecryptor);
        signer.init();

        // 5. Signer 기능 검증
        assertTrue(signer.isReady());
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, signer.getType());

        String message = "important-message";
        String signature = signer.sign(message);

        assertNotNull(signature);
        assertTrue(signer.verify(message, signature));
        assertFalse(signer.verify("tampered-message", signature));
    }
}
