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
 * AES-128 CBC Encryptor (SecretsKeyResolver 기반)
 * - 키는 SecretsManager로부터 주입되며, 다중 알고리즘 키 구조를 지원
 * - 키 조회 시 CryptoAlgorithmType 기반으로 식별
 */
@Slf4j
@Component("aes128Encryptor")
@RequiredArgsConstructor
public class Aes128Encryptor implements Encryptor {

    private static final int KEY_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final byte VERSION = 0x01;
    private static final String KEY_NAME = CryptoAlgorithmType.AES128.name();

    private final SecretsKeyResolver secretsKeyResolver;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] key;

    /**
     * 애플리케이션 부팅 시 최초 키 초기화
     * - SecretsKeyResolver에서 AES128 키를 조회하여 세팅
     */
    @PostConstruct
    public void init() {
        try {
            this.key = secretsKeyResolver.getCurrentKey(KEY_NAME);

            if (key == null || key.length != KEY_LENGTH) {
                throw new IllegalArgumentException(
                        String.format("AES-128 key must be exactly %d bytes. Found: %s",
                                KEY_LENGTH, (key == null ? "null" : key.length + " bytes")));
            }

            log.info("[Aes128Encryptor] AES-128 key [{}] loaded successfully.", KEY_NAME);
        } catch (Exception e) {
            log.error("[Aes128Encryptor] Failed to load AES-128 key: {}", e.getMessage(), e);
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
     * AES-128 CBC 방식으로 평문 암호화
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

            byte[] cipher = Aes128Engine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alg", "AES-CBC");
            payload.put("ver", VERSION);
            payload.put("iv", Base64Utils.encodeUrlSafe(iv));
            payload.put("cipher", Base64Utils.encodeUrlSafe(cipher));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[Aes128Encryptor] Encryption failed: {}", e.getMessage(), e);
            throw new EncryptException("AES-128 encryption failed", e);
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
                throw new DecryptException("Unsupported AES-128 encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("iv")));
            byte[] cipher = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("cipher")));
            byte[] plain = Aes128Engine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[Aes128Encryptor] Decryption failed: {}", e.getMessage(), e);
            throw new DecryptException("AES-128 decryption failed", e);
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
        return CryptoAlgorithmType.AES128;
    }

    /**
     * Encryptor가 준비되었는지 보장
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("AES-128 encryptor not initialized. Key is missing.");
        }
    }
}
