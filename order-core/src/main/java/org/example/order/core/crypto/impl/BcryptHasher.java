package org.example.order.core.crypto.impl;

import org.example.order.core.crypto.Hasher;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.engine.BcryptEngine;
import org.springframework.stereotype.Component;

@Component("bcryptHasher")
public class BcryptHasher implements Hasher {

    @Override
    public String hash(String plainText) {
        return BcryptEngine.hash(plainText);
    }

    @Override
    public boolean matches(String plainText, String hashed) {
        return BcryptEngine.verify(plainText, hashed);
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.BCRYPT;
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
