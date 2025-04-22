package org.example.order.core.infra.crypto.algorithm.hasher;

import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
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
