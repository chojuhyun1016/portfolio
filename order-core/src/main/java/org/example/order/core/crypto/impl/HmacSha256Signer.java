package org.example.order.core.crypto.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.Base64Utils;
import org.example.order.core.crypto.Signer;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.engine.HmacSha256Engine;
import org.example.order.core.crypto.exception.InvalidKeyException;
import org.example.order.core.crypto.exception.SignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component("hmacSha256Signer")
public class HmacSha256Signer implements Signer {

    private static final int MIN_KEY_LENGTH = 16;

    private byte[] secretKey;

    public HmacSha256Signer(@Value("${crypto.hmac.key:}") String configuredKey) {
        if (configuredKey != null && !configuredKey.isBlank()) {
            try {
                setKey(configuredKey);
            } catch (IllegalArgumentException e) {
                log.warn("HMAC-SHA256 key is invalid: {}", e.getMessage());
                // secretKey remains null (isReady false)
            }
        } else {
            log.info("HMAC-SHA256 key is not configured. This signer will be inactive.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        byte[] decoded = Base64Utils.decodeUrlSafe(base64Key);

        if (decoded.length < MIN_KEY_LENGTH) {
            throw new IllegalArgumentException("HMAC-SHA256 key must be at least 16 bytes.");
        }

        this.secretKey = decoded;
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
            log.error("HMAC-SHA256 sign failed: {}", e.getMessage(), e);
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
