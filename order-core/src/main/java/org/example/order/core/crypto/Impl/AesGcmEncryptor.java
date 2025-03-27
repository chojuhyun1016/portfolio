package org.example.order.core.crypto.Impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.Base64Utils;
import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.engine.AesGcmEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

@Slf4j
@Component("aesGcmEncryptor")
public class AesGcmEncryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12; // GCM 권장 IV
    private byte[] key;

    private final SecureRandom random = new SecureRandom();

    @Value("${encrypt.aesgcm.key}")
    private String base64Key;

    @PostConstruct
    public void init() {
        setKeyFromBase64(base64Key);
    }

    @Override
    public void setKeyFromBase64(String base64Key) {
        byte[] decoded = Base64Utils.decode(base64Key);

        if (decoded.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-GCM key must be 32 bytes.");
        }

        this.key = decoded;
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            byte[] cipher = AesGcmEngine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);
            byte[] combined = new byte[iv.length + cipher.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipher, 0, combined, iv.length, cipher.length);

            return Base64Utils.encode(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt failed", e);
        }
    }

    @Override
    public String decrypt(String base64CipherText) {
        try {
            byte[] decoded = Base64Utils.decode(base64CipherText);
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] cipher = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);
            byte[] plain = AesGcmEngine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return key != null;
    }
}
