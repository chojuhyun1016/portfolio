package org.example.order.core.infra.crypto.util;

import org.example.order.core.infra.crypto.code.CryptoAlgorithmType;
import org.example.order.common.utils.encode.Base64Utils;

import java.security.SecureRandom;

public class EncryptionKeyGenerator {

    // 안전한 키 생성을 위한 SecureRandom
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateKey(CryptoAlgorithmType type) {
        int keyLength;

        switch (type) {
            case AES128         -> keyLength = 16;  // 128-bit
            case AES256, AESGCM -> keyLength = 32;  // 256-bit
            case HMAC_SHA256    -> keyLength = 32;  // 일반적으로 32~64 byte 권장
            default -> throw new IllegalArgumentException("Unsupported algorithm for key generation: " + type);
        }

        byte[] keyBytes = new byte[keyLength];
        secureRandom.nextBytes(keyBytes);

        return Base64Utils.encodeUrlSafe(keyBytes);
    }
}
