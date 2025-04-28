package org.example.order.core.infra.crypto.algorithm.signer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.exception.InvalidKeyException;
import org.example.order.core.infra.crypto.exception.SignException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * HMAC-SHA256 서명/검증 Signer
 */
@Slf4j
@Component("hmacSha256Signer")
@RequiredArgsConstructor
public class HmacSha256Signer implements Signer {

    private static final int MIN_KEY_LENGTH = 16; // 최소 128비트 보장
    private final SecretsKeyResolver secretsKeyResolver;

    private byte[] secretKey;

    /**
     * 부팅 시 Secrets Manager로부터 키 로드
     */
    @PostConstruct
    public void init() {
        this.secretKey = secretsKeyResolver.getCurrentKey();

        if (secretKey.length < MIN_KEY_LENGTH) {
            throw new IllegalArgumentException("HMAC-SHA256 key must be at least 16 bytes.");
        }

        log.info("[Signer] HMAC-SHA256 key initialized successfully.");
    }

    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. Use Secrets Manager auto-load.");
    }

    @Override
    public String sign(String message) {
        if (!isReady()) {
            throw new InvalidKeyException("HMAC-SHA256 signer key not initialized.");
        }

        try {
            byte[] rawSignature = HmacSha256Engine.sign(message.getBytes(StandardCharsets.UTF_8), secretKey);
            return Base64Utils.encodeUrlSafe(rawSignature);
        } catch (Exception e) {
            log.error("HMAC-SHA256 sign failed", e);
            throw new SignException("HMAC-SHA256 sign failed", e);
        }
    }

    @Override
    public boolean verify(String message, String signature) {
        if (!isReady()) {
            throw new InvalidKeyException("HMAC-SHA256 signer key not initialized.");
        }

        return sign(message).equals(signature);
    }

    @Override
    public boolean isReady() {
        return secretKey != null;
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.HMAC_SHA256;
    }
}
