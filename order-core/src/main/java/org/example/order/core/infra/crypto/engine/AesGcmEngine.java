package org.example.order.core.infra.crypto.engine;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class AesGcmEngine {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int REQUIRED_KEY_LENGTH = 32;   // 256-bit
    private static final int REQUIRED_IV_LENGTH = 12;    // GCM 권장

    /**
     * AES-GCM 암호화
     *
     * @param plainText 평문 바이트
     * @param key       32바이트 AES 키
     * @param iv        12바이트 IV
     * @return 암호문 바이트
     * @throws GeneralSecurityException 암호화 실패 시 예외
     */
    public static byte[] encrypt(byte[] plainText, byte[] key, byte[] iv) throws GeneralSecurityException {
        validate(key, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);

        return cipher.doFinal(plainText);
    }

    /**
     * AES-GCM 복호화
     *
     * @param cipherText 암호문 바이트
     * @param key        32바이트 AES 키
     * @param iv         12바이트 IV
     * @return 평문 바이트
     * @throws GeneralSecurityException 복호화 실패 시 예외
     */
    public static byte[] decrypt(byte[] cipherText, byte[] key, byte[] iv) throws GeneralSecurityException {
        validate(key, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

        return cipher.doFinal(cipherText);
    }

    /**
     * 키와 IV의 유효성을 검증
     */
    private static void validate(byte[] key, byte[] iv) {
        Objects.requireNonNull(key, "AES-GCM key must not be null");
        Objects.requireNonNull(iv, "AES-GCM IV must not be null");

        if (key.length != REQUIRED_KEY_LENGTH) {
            throw new IllegalArgumentException("AES-GCM key must be 32 bytes");
        }

        if (iv.length != REQUIRED_IV_LENGTH) {
            throw new IllegalArgumentException("AES-GCM IV must be 12 bytes");
        }
    }
}
