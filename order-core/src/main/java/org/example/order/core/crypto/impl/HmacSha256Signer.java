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

@Slf4j
@Component("hmacSha256Signer")
public class HmacSha256Signer implements Signer {

    private final String secretKey;

    public HmacSha256Signer(@Value("${crypto.hmac.key:}") String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            log.warn("HMAC-SHA256 secretKey not configured or empty. Signer will not be usable.");
            this.secretKey = null;
        } else {
            this.secretKey = configuredKey;
        }
    }

    @Override
    public String sign(String message) {
        if (!isReady()) {
            throw new InvalidKeyException(secretKey);
        }

        try {
            byte[] signature = HmacSha256Engine.sign(message, secretKey);

            return Base64Utils.encodeUrlSafe(signature);
        } catch (Exception e) {
            log.error("HMAC-SHA256 sign failed: {}", e.getMessage(), e);
            throw new SignException("HMAC-SHA256 sign failed", e);
        }
    }

    @Override
    public boolean verify(String message, String signature) {
        if (!isReady()) {
            throw new InvalidKeyException(secretKey);
        }

        return sign(message).equals(signature);
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.HMAC_SHA256;
    }

    @Override
    public boolean isReady() {
        return secretKey != null && !secretKey.isBlank();
    }
}
