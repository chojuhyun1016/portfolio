package org.example.order.core.crypto;

import org.example.order.core.crypto.Impl.AesGcmEncryptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorTest {

    @Test
    void generateRandomKeyBase64_shouldReturnBase64EncodedKeyOfProperLength() {
        Encryptor encryptor =  new AesGcmEncryptor();
        String key = encryptor.generateRandomKeyBase64();
        assertNotNull(key);
        byte[] decoded = java.util.Base64.getDecoder().decode(key);
        assertEquals(32, decoded.length); // 256-bit = 32 bytes

        String base64Key = encryptor.generateRandomKeyBase64();

        // Base64 디코딩하여 길이 확인 (AES-256: 32 bytes)
        decoded = java.util.Base64.getDecoder().decode(base64Key);

        System.out.println("🔐 Generated Base64 Key: " + base64Key);
        System.out.println("🔐 Decoded Key Length: " + decoded.length + " bytes");

        assertNotNull(base64Key);
        assertEquals(32, decoded.length, "Key must be 32 bytes for AES-256");
    }
}
