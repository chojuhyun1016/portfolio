package org.example.order.core.crypto;

import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.impl.HmacSha256Signer;
import org.example.order.core.crypto.util.EncryptionKeyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignerTest {

    @Test
    void testHmacSha256SignAndVerify() {
        String secretKey = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.HMAC_SHA256);
        Signer signer = new HmacSha256Signer(secretKey);

        assertTrue(signer.isReady());
        assertEquals(CryptoAlgorithmType.HMAC_SHA256, signer.getType());

        String message = "important-message";
        String signature = signer.sign(message);

        assertNotNull(signature);
        assertTrue(signer.verify(message, signature));
        assertFalse(signer.verify("tampered-message", signature));
    }
}
