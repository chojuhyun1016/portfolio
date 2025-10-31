package org.example.order.common.helper.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecureHashUtils {
    public static String getMD5Hash(String input) {
        return getHash(input, "MD5");
    }

    public static String getSHA256Hash(String input) {
        return getHash(input, "SHA-256");
    }

    private static String getHash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(input.getBytes());
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid hashing algorithm: " + algorithm, e);
        }
    }
}
