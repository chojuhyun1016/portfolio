package org.example.order.core.infra.crypto.algorithm.signer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.exception.InvalidKeyException;
import org.example.order.core.infra.crypto.exception.SignException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * HMAC-SHA256 서명/검증 Signer (SecretsKeyResolver 기반)
 * - AWS Secrets Manager 연동 (키매니저1)
 * - 다중 알고리즘 구조 지원
 */
@Slf4j
@Component("hmacSha256Signer")
@RequiredArgsConstructor
public class HmacSha256Signer implements Signer {

    private static final int MIN_KEY_LENGTH = 16;  // 최소 128비트 보장
    private static final String KEY_NAME = CryptoAlgorithmType.HMAC_SHA256.name();  // 키 식별자

    private final SecretsKeyResolver secretsKeyResolver;
    private byte[] secretKey;

    /**
     * 부팅 시 Secrets Manager로부터 키 로드
     */
    @PostConstruct
    public void init() {
        try {
            this.secretKey = secretsKeyResolver.getCurrentKey(KEY_NAME);

            if (secretKey == null || secretKey.length < MIN_KEY_LENGTH) {
                throw new IllegalArgumentException(
                        String.format("HMAC-SHA256 key [%s] must be at least %d bytes. Found: %s",
                                KEY_NAME, MIN_KEY_LENGTH, (secretKey == null ? "null" : secretKey.length + " bytes")));
            }

            log.info("[HmacSha256Signer] HMAC-SHA256 key [{}] initialized successfully.", KEY_NAME);
        } catch (Exception e) {
            log.error("[HmacSha256Signer] Failed to initialize key [{}]: {}", KEY_NAME, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 외부에서 수동으로 키를 설정하는 방식은 허용하지 않음
     */
    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. Use Secrets Manager integration.");
    }

    /**
     * 메시지를 HMAC-SHA256 방식으로 서명
     *
     * @param message 서명할 메시지
     * @return URL-safe Base64로 인코딩된 서명 문자열
     */
    @Override
    public String sign(String message) {
        ensureReady();

        try {
            byte[] rawSignature = HmacSha256Engine.sign(message.getBytes(StandardCharsets.UTF_8), secretKey);
            return Base64Utils.encodeUrlSafe(rawSignature);
        } catch (Exception e) {
            log.error("[HmacSha256Signer] Signing failed: {}", e.getMessage(), e);
            throw new SignException("HMAC-SHA256 sign failed", e);
        }
    }

    /**
     * 메시지와 서명이 일치하는지 검증
     *
     * @param message   원본 메시지
     * @param signature 검증할 서명
     * @return 검증 결과 (true=정상)
     */
    @Override
    public boolean verify(String message, String signature) {
        ensureReady();
        return sign(message).equals(signature);
    }

    /**
     * 준비 상태 확인
     */
    @Override
    public boolean isReady() {
        return secretKey != null;
    }

    /**
     * 알고리즘 타입 반환
     */
    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.HMAC_SHA256;
    }

    /**
     * Signer 준비 상태를 보장
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new InvalidKeyException(
                    String.format("HMAC-SHA256 signer [%s] key not initialized.", KEY_NAME)
            );
        }
    }
}
