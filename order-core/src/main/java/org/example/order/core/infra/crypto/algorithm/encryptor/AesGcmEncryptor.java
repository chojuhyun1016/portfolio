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
 * AES-GCM 256 암호화/복호화 Encryptor
 */
@Slf4j
@Component("aesGcmEncryptor")
@RequiredArgsConstructor
public class AesGcmEncryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;    // AES-256: 32 bytes
    private static final int IV_LENGTH = 12;     // GCM 권장 IV: 12 bytes
    private static final byte VERSION = 0x01;    // 암호화 버전 관리

    private final SecretsKeyResolver secretsKeyResolver; // Secrets Manager 키 핫스왑 관리
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] key;

    /**
     * 부팅 시 Secrets Manager에서 키 로드
     */
    @PostConstruct
    public void init() {
        this.key = secretsKeyResolver.getCurrentKey();

        if (key.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-GCM key must be 32 bytes.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. Use Secrets Manager auto-load.");
    }

    @Override
    public String encrypt(String plainText) {
        if (!isReady()) {
            throw new EncryptException("AES-GCM encryptor not initialized. Key missing.");
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
            log.error("AES-GCM encryption failed", e);
            throw new EncryptException("AES-GCM encryption failed", e);
        }
    }

    @Override
    public String decrypt(String json) {
        if (!isReady()) {
            throw new DecryptException("AES-GCM decryptor not initialized. Key missing.");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);
            byte version = Byte.parseByte(String.valueOf(payload.get("ver")));

            if (version != VERSION) {
                throw new DecryptException("Unsupported AES-GCM encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("iv")));
            byte[] cipher = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("cipher")));

            byte[] plain = AesGcmEngine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-GCM decryption failed", e);
            throw new DecryptException("AES-GCM decryption failed", e);
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
