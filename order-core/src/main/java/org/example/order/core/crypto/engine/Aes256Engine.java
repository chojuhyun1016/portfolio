package org.example.order.core.crypto.engine;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * AES-256 CBC 암호화/복호화 엔진
 */
public class Aes256Engine {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int REQUIRED_KEY_LENGTH = 32; // 256-bit
    private static final int REQUIRED_IV_LENGTH = 16;  // 128-bit

    /**
     * AES-256 CBC 암호화
     *
     * @param plainText 평문 바이트
     * @param key       32바이트 AES 키
     * @param iv        16바이트 IV
     * @return 암호문 바이트
     * @throws GeneralSecurityException 암호화 실패 시 예외
     */
    public static byte[] encrypt(byte[] plainText, byte[] key, byte[] iv) throws GeneralSecurityException {
        validate(key, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), new IvParameterSpec(iv));

        return cipher.doFinal(plainText);
    }

    /**
     * AES-256 CBC 복호화
     *
     * @param cipherText 암호문 바이트
     * @param key        32바이트 AES 키
     * @param iv         16바이트 IV
     * @return 평문 바이트
     * @throws GeneralSecurityException 복호화 실패 시 예외
     */
    public static byte[] decrypt(byte[] cipherText, byte[] key, byte[] iv) throws GeneralSecurityException {
        validate(key, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), new IvParameterSpec(iv));

        return cipher.doFinal(cipherText);
    }

    /**
     * 키와 IV 유효성 검증
     */
    private static void validate(byte[] key, byte[] iv) {
        Objects.requireNonNull(key, "AES-256 key must not be null");
        Objects.requireNonNull(iv, "AES-256 IV must not be null");

        if (key.length != REQUIRED_KEY_LENGTH) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes");
        }

        if (iv.length != REQUIRED_IV_LENGTH) {
            throw new IllegalArgumentException("AES-256 IV must be 16 bytes");
        }
    }
}
