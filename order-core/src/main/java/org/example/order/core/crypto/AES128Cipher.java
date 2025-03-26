package org.example.order.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AES128Cipher {

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_IV_LENGTH = 16;

    public static String encrypt(String plainText, String key, String iv) throws Exception {
        validateKeyAndIv(key, iv);

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return toHex(encrypted);
    }

    public static String decrypt(String cipherHex, String key, String iv) throws Exception {
        validateKeyAndIv(key, iv);

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(fromHex(cipherHex));

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // HEX -> byte[]
    public static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }

        return result;
    }

    // byte[] -> HEX
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    // 키와 IV 유효성 검사
    private static void validateKeyAndIv(String key, String iv) {
        if (key == null || key.length() != KEY_IV_LENGTH) {
            throw new IllegalArgumentException("Key must be 16 characters long (128-bit)");
        }

        if (iv == null || iv.length() != KEY_IV_LENGTH) {
            throw new IllegalArgumentException("IV must be 16 characters long (128-bit)");
        }
    }
}
