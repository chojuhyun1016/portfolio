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
 * AES-256 CBC 암호화/복호화 Encryptor (SecretsKeyResolver 기반)
 * - Secrets Manager에서 다중 키를 관리하며, AES-256 키를 조회해 사용
 */
@Slf4j
@Component("aes256Encryptor")
@RequiredArgsConstructor
public class Aes256Encryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;      // 32바이트 (256비트)
    private static final int IV_LENGTH = 16;       // 16바이트 (128비트)
    private static final byte VERSION = 0x01;      // 암호화 버전 관리
    private static final String KEY_NAME = CryptoAlgorithmType.AES256.name();  // 키 식별자

    private final SecretsKeyResolver secretsKeyResolver;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] key;  // 현재 사용 중인 AES-256 키

    /**
     * 부팅 시 Secrets Manager에서 AES-256 키를 가져와 초기화
     */
    @PostConstruct
    public void init() {
        try {
            this.key = secretsKeyResolver.getCurrentKey(KEY_NAME);

            if (key == null || key.length != KEY_LENGTH) {
                throw new IllegalArgumentException(
                        String.format("AES-256 key must be exactly %d bytes. Found: %s",
                                KEY_LENGTH, (key == null ? "null" : key.length + " bytes")));
            }

            log.info("[Aes256Encryptor] AES-256 key [{}] loaded successfully.", KEY_NAME);
        } catch (Exception e) {
            log.error("[Aes256Encryptor] Failed to load AES-256 key: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * setKey는 외부 초기화를 허용하지 않음
     */
    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. SecretsKeyResolver is used for key management.");
    }

    /**
     * AES-256 CBC 방식으로 평문 암호화
     *
     * @param plainText 평문
     * @return JSON 형식의 암호화 결과
     */
    @Override
    public String encrypt(String plainText) {
        ensureReady();

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
            log.error("[Aes256Encryptor] Encryption failed: {}", e.getMessage(), e);
            throw new EncryptException("AES-256 encryption failed", e);
        }
    }

    /**
     * JSON 형식의 암호화 데이터를 복호화
     *
     * @param json 암호화된 JSON 문자열
     * @return 복호화된 평문
     */
    @Override
    public String decrypt(String json) {
        ensureReady();

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
            log.error("[Aes256Encryptor] Decryption failed: {}", e.getMessage(), e);
            throw new DecryptException("AES-256 decryption failed", e);
        }
    }

    /**
     * Encryptor 준비 상태 확인
     */
    @Override
    public boolean isReady() {
        return key != null;
    }

    /**
     * 알고리즘 타입 반환
     */
    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.AES256;
    }

    /**
     * Encryptor가 준비되었는지 보장
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("AES-256 encryptor not initialized. Key is missing.");
        }
    }
}
