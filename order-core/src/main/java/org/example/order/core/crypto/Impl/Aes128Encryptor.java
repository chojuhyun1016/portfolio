package org.example.order.core.crypto.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.Base64Utils;
import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.code.EncryptorType;
import org.example.order.core.crypto.engine.Aes128Engine;
import org.example.order.core.crypto.exception.DecryptException;
import org.example.order.core.crypto.exception.EncryptException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("aes128Encryptor")
public class Aes128Encryptor implements Encryptor {

    private static final int KEY_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final byte VERSION = 0x01;

    private byte[] key;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Aes128Encryptor(@Value("${encrypt.aes128.key:}") String base64Key) {
        if (base64Key != null && !base64Key.isBlank()) {
            try {
                setKey(base64Key);
            } catch (IllegalArgumentException e) {
                log.warn("AES-128 key is invalid: {}", e.getMessage());
            }
        } else {
            log.info("AES-128 key is not set. This encryptor will not be ready.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        byte[] decoded = Base64Utils.decodeUrlSafe(base64Key);

        if (decoded.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-128 key must be 16 bytes.");
        }

        this.key = decoded;
    }

    @Override
    public String encrypt(String plainText) {
        if (!isReady()) {
            throw new EncryptException("AES-128 encryptor not initialized. Key missing.");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            byte[] cipher = Aes128Engine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alg", "AES-CBC");
            payload.put("ver", VERSION);
            payload.put("iv", Base64Utils.encodeUrlSafe(iv));
            payload.put("cipher", Base64Utils.encodeUrlSafe(cipher));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("AES-128 encryption failed: {}", e.getMessage(), e);
            throw new EncryptException("AES-128 encryption failed", e);
        }
    }

    @Override
    public String decrypt(String json) {
        if (!isReady()) {
            throw new DecryptException("AES-128 decryptor not initialized. Key missing.");
        }

        try {
            Map<String, String> payload = objectMapper.readValue(json, Map.class);

            byte version = Byte.parseByte(String.valueOf(payload.get("ver")));
            if (version != VERSION) {
                throw new DecryptException("Unsupported AES-128 encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(payload.get("iv"));
            byte[] cipher = Base64Utils.decodeUrlSafe(payload.get("cipher"));
            byte[] plain = Aes128Engine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-128 decryption failed: {}", e.getMessage(), e);
            throw new DecryptException("AES-128 decryption failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return key != null;
    }

    @Override
    public EncryptorType getType() {
        return EncryptorType.AES128;
    }
}
