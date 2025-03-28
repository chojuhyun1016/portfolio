package org.example.order.core.crypto.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.Base64Utils;
import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.code.EncryptorType;
import org.example.order.core.crypto.engine.Aes128Engine;
import org.example.order.core.crypto.exception.DecryptException;
import org.example.order.core.crypto.exception.EncryptException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("aes128Encryptor")
public class Aes128Encryptor implements Encryptor, InitializingBean {

    private static final int KEY_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final byte VERSION = 0x01;

    private byte[] key;
    private final SecureRandom random = new SecureRandom();

    @Value("${encrypt.aes128.key}")
    private String base64Key;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterPropertiesSet() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("AES-128 key is not set. Please check configuration.");
        }

        setKey(base64Key);
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
            throw new EncryptException("Encryptor not initialized properly. Missing key.");
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
            log.error("Encryption failed: {}", e.getMessage(), e);
            throw new EncryptException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String json) {
        if (!isReady()) {
            throw new DecryptException("Encryptor not initialized properly. Missing key.");
        }

        try {
            Map<String, String> payload = objectMapper.readValue(json, Map.class);

            byte version = Byte.parseByte(String.valueOf(payload.get("ver")));
            if (version != VERSION) throw new DecryptException("Unsupported encryption version: " + version);

            byte[] iv = Base64Utils.decodeUrlSafe(payload.get("iv"));
            byte[] cipher = Base64Utils.decodeUrlSafe(payload.get("cipher"));

            byte[] plain = Aes128Engine.decrypt(cipher, key, iv);

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
    public EncryptorType getType() {
        return EncryptorType.AES128;
    }
}
