package org.example.order.core.infra.crypto.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.crypto.Signer;
import org.example.order.core.infra.crypto.code.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.engine.HmacSha256Engine;
import org.example.order.core.infra.crypto.exception.InvalidKeyException;
import org.example.order.core.infra.crypto.exception.SignException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component("hmacSha256Signer")
public class HmacSha256Signer implements Signer {

    private static final int MIN_KEY_LENGTH = 16;

    private byte[] secretKey;

    public HmacSha256Signer(EncryptProperties encryptProperties) {
        String configuredKey = encryptProperties.getHmac().getKey();
        if (configuredKey != null && !configuredKey.isBlank()) {
            try {
                setKey(configuredKey);
            } catch (IllegalArgumentException e) {
                log.warn("HMAC-SHA256 key is invalid: {}", e.getMessage());
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
