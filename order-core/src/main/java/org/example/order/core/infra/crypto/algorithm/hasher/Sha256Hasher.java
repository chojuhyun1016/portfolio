package org.example.order.core.infra.crypto.algorithm.hasher;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.exception.HashException;
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
