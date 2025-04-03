package org.example.order.core.crypto.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.config.EncryptProperties;
import org.example.order.core.crypto.engine.AesGcmEngine;
import org.example.order.core.crypto.exception.DecryptException;
import org.example.order.core.crypto.exception.EncryptException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("aesGcmEncryptor")
public class AesGcmEncryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final byte VERSION = 0x01;

    private byte[] key;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AesGcmEncryptor(EncryptProperties encryptProperties) {
        String base64Key = encryptProperties.getAesgcm().getKey();
        if (base64Key != null && !base64Key.isBlank()) {
            try {
                setKey(base64Key);
            } catch (IllegalArgumentException e) {
                log.warn("AES-GCM key is invalid: {}", e.getMessage());
            }
        } else {
            log.info("AES-GCM key not configured. This encryptor will be inactive.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        byte[] decoded = Base64Utils.decodeUrlSafe(base64Key);

        if (decoded.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-GCM key must be 32 bytes.");
        }

        this.key = decoded;
    }

    @Override
    public String encrypt(String plainText) {
        if (!isReady()) {
            throw new EncryptException("AES-GCM key not initialized. Cannot encrypt.");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            byte[] cipher = AesGcmEngine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alg", "AES-GCM");
            payload.put("ver", VERSION);
            payload.put("iv", Base64Utils.encodeUrlSafe(iv));
            payload.put("cipher", Base64Utils.encodeUrlSafe(cipher));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage(), e);
            throw new EncryptException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String json) {
        if (!isReady()) {
            throw new DecryptException("AES-GCM key not initialized. Cannot decrypt.");
        }

        try {
            Map<String, String> payload = objectMapper.readValue(json, Map.class);

            byte version = Byte.parseByte(String.valueOf(payload.get("ver")));
            if (version != VERSION) {
                throw new DecryptException("Unsupported encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(payload.get("iv"));
            byte[] cipher = Base64Utils.decodeUrlSafe(payload.get("cipher"));

            byte[] plain = AesGcmEngine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage(), e);
            throw new DecryptException("Decryption failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return key != null;
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.AESGCM;
    }
}
