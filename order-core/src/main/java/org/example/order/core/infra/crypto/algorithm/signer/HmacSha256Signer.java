package org.example.order.core.infra.crypto.algorithm.signer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.encode.Base64Utils;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.exception.InvalidKeyException;
import org.example.order.core.infra.crypto.exception.SignException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component("hmacSha256Signer")
@RequiredArgsConstructor
public class HmacSha256Signer implements Signer {

    private static final int MIN_KEY_LENGTH = 16;

    private final EncryptProperties encryptProperties;
    private final KmsDecryptor kmsDecryptor;
    private byte[] secretKey;

    @PostConstruct
    public void init() {
        String base64Key = encryptProperties.getHmac().getKey();
        if (base64Key != null && !base64Key.isBlank()) {
            try {
                byte[] keyBytes = kmsDecryptor.decryptBase64EncodedKey(base64Key);
                if (keyBytes.length < MIN_KEY_LENGTH) {
                    throw new IllegalArgumentException("HMAC-SHA256 key must be at least 16 bytes.");
                }
                this.secretKey = keyBytes;
                log.info("[Signer] HMAC-SHA256 key initialized.");
            } catch (Exception e) {
                log.warn("[Signer] Failed to initialize HMAC-SHA256 key: {}", e.getMessage(), e);
            }
        } else {
            log.info("[Signer] HMAC-SHA256 key not configured.");
        }
    }

    @Override
    public void setKey(String base64Key) {
        throw new UnsupportedOperationException("setKey is not supported. Use KMS-based initialization.");
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
