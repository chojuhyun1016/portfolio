package org.example.order.core.infra.crypto.algorithm.encryptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.exception.DecryptException;
import org.example.order.core.infra.crypto.exception.EncryptException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * AES-256 CBC 암호화/복호화 Encryptor
 */
@Slf4j
@Component("aes256Encryptor")
@RequiredArgsConstructor
public class Aes256Encryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;      // 32바이트 (256비트)
    private static final int IV_LENGTH = 16;       // 16바이트 (128비트)
    private static final byte VERSION = 0x01;      // 암호화 버전 관리

    private final SecretsKeyResolver secretsKeyResolver; // Secrets Manager 기반 키 관리
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] key;  // 현재 사용 중인 키

    /**
     * 부팅 시 Secrets Manager에서 키를 가져와 초기화
     */
    @PostConstruct
    public void init() {
        this.key = secretsKeyResolver.getCurrentKey();

        if (key.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. Use Secrets Manager auto-load.");
    }

    @Override
    public String encrypt(String plainText) {
        if (!isReady()) {
            throw new EncryptException("AES-256 encryptor not initialized. Key missing.");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            byte[] cipher = Aes256Engine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alg", "AES-CBC");
            payload.put("ver", VERSION);
            payload.put("iv", Base64Utils.encodeUrlSafe(iv));
            payload.put("cipher", Base64Utils.encodeUrlSafe(cipher));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("AES-256 encryption failed", e);
            throw new EncryptException("AES-256 encryption failed", e);
        }
    }

    @Override
    public String decrypt(String json) {
        if (!isReady()) {
            throw new DecryptException("AES-256 decryptor not initialized. Key missing.");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);
            byte version = Byte.parseByte(String.valueOf(payload.get("ver")));

            if (version != VERSION) {
                throw new DecryptException("Unsupported AES-256 encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("iv")));
            byte[] cipher = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("cipher")));
            byte[] plain = Aes256Engine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-256 decryption failed", e);
            throw new DecryptException("AES-256 decryption failed", e);
        }
    }

    @Override
    public boolean isReady() {
        return key != null;
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.AES256;
    }
}
