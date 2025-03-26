package org.example.order.core.crypto.Impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.crypto.Encryptor;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class Aes128Encryptor implements Encryptor {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int AES_128_KEY_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    @Value("${encrypt.aes128.key:}")
    private String base64EncodedKey;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (base64EncodedKey == null || base64EncodedKey.isBlank()) {
            throw new IllegalStateException("Encryption key is not configured.");
        }

        setKeyFromBase64(base64EncodedKey);
    }

    @Override
    public void setKeyFromBase64(String base64Key) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);

            if (decodedKey.length != AES_128_KEY_LENGTH) {
                throw new IllegalArgumentException("AES-128 key must be 16 bytes.");
            }

            this.secretKey = new SecretKeySpec(decodedKey, "AES");

            log.info("Aes128Encryptor initialized with AES-128 key.");
        } catch (Exception e) {
            log.error("Failed to set AES-128 key", e);

            throw new RuntimeException("Invalid key provided", e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES-128 encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String base64Text) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Text);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);
            byte[] cipherText = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-128 decryption failed", e);

            throw new RuntimeException("Decryption failed", e);
        }
    }

    @Override
    public String hash(String input, String algorithm) {
        if (input == null || input.isBlank()) {
            return input;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing failed", e);

            throw new RuntimeException("Hashing failed", e);
        }
    }

    @Override
    public boolean isEncryptorReady() {
        return secretKey != null;
    }

    @Override
    public String generateRandomKeyBase64() {
        byte[] key = new byte[AES_128_KEY_LENGTH];
        secureRandom.nextBytes(key);

        return Base64.getEncoder().encodeToString(key);
    }
}
