package org.example.order.core.crypto.Impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.crypto.Encryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AesGcmEncryptor implements Encryptor {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int AES_256_KEY_LENGTH = 32;

    @Value("${encrypt.aesgcm.key:}")
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

            if (decodedKey.length != AES_256_KEY_LENGTH) {
                throw new IllegalArgumentException("AES-256 key must be 32 bytes.");
            }

            this.secretKey = new SecretKeySpec(decodedKey, "AES");

            log.info("AesGcmEncryptor initialized with AES-256 key.");
        } catch (Exception e) {
            log.error("Failed to set encryption key from Base64", e);

            throw new RuntimeException("Invalid key provided", e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);

            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String base64Text) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Text);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);

            throw new RuntimeException("Decryption failed", e);
        }
    }

    @Override
    public String hash(String input, String algorithm) {
        if (input == null || input.isBlank()) return input;

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing failed with algorithm {}", algorithm, e);

            throw new RuntimeException("Hashing failed", e);
        }
    }

    @Override
    public boolean isEncryptorReady() {
        return secretKey != null;
    }

    /** 🔐 랜덤 AES-256 키 생성 (Base64 인코딩 반환) */
    public String generateRandomKeyBase64() {
        byte[] key = new byte[AES_256_KEY_LENGTH];
        new SecureRandom().nextBytes(key);

        return Base64.getEncoder().encodeToString(key);
    }
}
