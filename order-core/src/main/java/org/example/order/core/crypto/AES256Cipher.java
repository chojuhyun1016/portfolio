package org.example.order.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256 암호화 유틸
 * - 키는 반드시 32바이트 (256bit)
 * - IV는 반드시 16바이트 (128bit)
 */
public class AES256Cipher {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private final SecretKeySpec secretKeySpec;
    private final IvParameterSpec ivParameterSpec;

    public AES256Cipher(String key) {
        if (key == null || key.length() < KEY_LENGTH) {
            throw new IllegalArgumentException("Key must be at least 32 characters for AES-256.");
        }

        String keyStr = key.substring(0, KEY_LENGTH);
        String ivStr = key.substring(0, IV_LENGTH);

        this.secretKeySpec = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), "AES");
        this.ivParameterSpec = new IvParameterSpec(ivStr.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String base64EncryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decodedBytes = Base64.getDecoder().decode(base64EncryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
