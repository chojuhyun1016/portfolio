package org.example.order.core.crypto.impl;

import org.example.order.common.utils.Base64Utils;
import org.example.order.core.crypto.Hasher;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.engine.Sha256Engine;
import org.example.order.core.crypto.exception.HashException;
import org.springframework.stereotype.Component;

@Component("sha256Hasher")
public class Sha256Hasher implements Hasher {

    @Override
    public String hash(String plainText) {
        try {
            return Base64Utils.encode(Sha256Engine.hash(plainText.getBytes()));
        } catch (Exception e) {
            throw new HashException("SHA-256 hash failed", e);
        }
    }

    @Override
    public boolean matches(String plainText, String hashed) {
        return hash(plainText).equals(hashed);
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.SHA256;
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
