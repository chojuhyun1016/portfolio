package org.example.order.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 암호화 유틸
 * - 키는 반드시 32바이트 (256bit)
 * - IV는 매 암호화 시 자동 생성 (12바이트 = 96bit)
 * - 결과 = Base64(IV + 암호문 + 태그)
 */
public class AESGCMCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 32; // 256bit
    private static final int IV_LENGTH = 12;  // 96bit (GCM 권장)
    private static final int TAG_LENGTH_BIT = 128; // 인증 태그 길이

    private final SecretKeySpec secretKeySpec;

    public AESGCMCipher(String key) {
        if (key == null || key.length() < KEY_LENGTH) {
            throw new IllegalArgumentException("Key must be at least 32 characters for AES-256-GCM.");
        }

        String keyStr = key.substring(0, KEY_LENGTH);
        this.secretKeySpec = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), "AES");
    }

    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // IV + encrypted => 결과
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String base64Encrypted) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Encrypted);

        // 분리: IV + 암호문+태그
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
