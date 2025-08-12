package org.example.order.core.infra.crypto.algorithm.encryptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.helper.encode.Base64Utils;
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
 * AES-GCM 256 Encryptor
 * - 키는 외부에서 setKey(base64)로 주입
 * - SecretsKeyResolver 등 외부 키 매니저에 의존하지 않음
 */
@Slf4j
@Component("aesGcmEncryptor")
public class AesGcmEncryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;  // 256-bit
    private static final int IV_LENGTH = 12;   // GCM 권장
    private static final byte VERSION = 0x01;

    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] key;

    /**
     * 외부에서 Base64(URL-safe) 키를 주입
     */
    @Override
    public void setKey(String base64Key) {
        try {
            byte[] k = Base64Utils.decodeUrlSafe(base64Key);
            if (k == null || k.length != KEY_LENGTH) {
                throw new IllegalArgumentException("AES-GCM key must be exactly 32 bytes.");
            }
            this.key = k;
            log.info("[AesGcmEncryptor] key set ({} bytes).", k.length);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AES-GCM base64 key.", e);
        }
    }

    /**
     * AES-GCM 방식으로 평문 암호화
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

            byte[] cipher = AesGcmEngine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alg", "AES-GCM");
            payload.put("ver", VERSION);
            payload.put("iv", Base64Utils.encodeUrlSafe(iv));
            payload.put("cipher", Base64Utils.encodeUrlSafe(cipher));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new EncryptException("AES-GCM encryption failed", e);
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
                throw new DecryptException("Unsupported AES-GCM encryption version: " + version);
            }

            byte[] iv = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("iv")));
            byte[] cipher = Base64Utils.decodeUrlSafe(String.valueOf(payload.get("cipher")));
            byte[] plain = AesGcmEngine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptException("AES-GCM decryption failed", e);
        }
    }

    /**
     * Encryptor 준비 상태 확인
     */
    @Override
    public boolean isReady() {
        return key != null && key.length == KEY_LENGTH;
    }

    /**
     * 알고리즘 타입 반환
     */
    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.AESGCM;
    }

    /**
     * Encryptor가 준비되었는지 보장
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("AES-GCM encryptor not initialized. Call setKey(base64) first.");
        }
    }
}
