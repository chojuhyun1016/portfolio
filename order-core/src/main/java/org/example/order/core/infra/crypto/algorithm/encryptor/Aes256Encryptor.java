package org.example.order.core.infra.crypto.algorithm.encryptor;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.exception.DecryptException;
import org.example.order.core.infra.crypto.exception.EncryptException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES-256 CBC Encryptor
 * - 키는 외부에서 setKey(base64)로 주입
 * - SecretsKeyResolver 등 외부 키 매니저에 의존하지 않음
 * - 포맷: "v1:" + Base64( IV(16B) || CIPHER )
 */
@Slf4j
public class Aes256Encryptor implements Encryptor {

    private static final int KEY_LENGTH = 32;  // 256-bit
    private static final int IV_LENGTH = 16;
    private static final String PREFIX = "v1:";

    private final SecureRandom random = new SecureRandom();

    private byte[] key;

    /**
     * 외부에서 Base64(표준) 키를 주입
     */
    @Override
    public void setKey(String base64Key) {
        try {
            byte[] k = Base64Utils.decode(base64Key);

            if (k == null || k.length != KEY_LENGTH) {
                throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes.");
            }

            this.key = k;

            log.info("[Aes256Encryptor] key set ({} bytes).", k.length);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AES-256 base64 key.", e);
        }
    }

    /**
     * AES-256 CBC 방식으로 평문 암호화
     *
     * @param plainText 평문
     * @return 콤팩트 형식의 암호화 결과: "v1:" + Base64(IV||CIPHER)
     */
    @Override
    public String encrypt(String plainText) {
        ensureReady();

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            byte[] cipher = Aes256Engine.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key, iv);

            byte[] pack = new byte[IV_LENGTH + cipher.length];
            System.arraycopy(iv, 0, pack, 0, IV_LENGTH);
            System.arraycopy(cipher, 0, pack, IV_LENGTH, cipher.length);

            return PREFIX + Base64Utils.encode(pack);
        } catch (Exception e) {
            throw new EncryptException("AES-256 encryption failed", e);
        }
    }

    /**
     * 콤팩트 형식의 암호화 데이터를 복호화
     *
     * @param token "v1:" + Base64(IV||CIPHER)
     * @return 복호화된 평문
     */
    @Override
    public String decrypt(String token) {
        ensureReady();

        try {
            if (token == null || !token.startsWith(PREFIX)) {
                throw new DecryptException("Unknown/unsupported token format");
            }

            String b64 = token.substring(PREFIX.length());
            byte[] pack = Base64Utils.decode(b64);

            if (pack.length <= IV_LENGTH) {
                throw new DecryptException("invalid token length");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipher = new byte[pack.length - IV_LENGTH];
            System.arraycopy(pack, 0, iv, 0, IV_LENGTH);
            System.arraycopy(pack, IV_LENGTH, cipher, 0, cipher.length);

            byte[] plain = Aes256Engine.decrypt(cipher, key, iv);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptException("AES-256 decryption failed", e);
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
        return CryptoAlgorithmType.AES256;
    }

    /**
     * Encryptor가 준비되었는지 보장
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("AES-256 encryptor not initialized. Call setKey(base64) first.");
        }
    }
}
