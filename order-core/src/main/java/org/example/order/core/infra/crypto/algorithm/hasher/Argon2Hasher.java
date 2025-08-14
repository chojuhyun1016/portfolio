package org.example.order.core.infra.crypto.algorithm.hasher;

import org.example.order.core.infra.crypto.contract.Hasher;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;

public class Argon2Hasher implements Hasher {

    @Override
    public String hash(String plainText) {
        char[] pw = plainText.toCharArray();

        try {
            return Argon2Engine.hash(pw);
        } finally {
            Argon2Engine.wipe(pw);
        }
    }

    @Override
    public boolean matches(String plainText, String hashed) {
        return Argon2Engine.verify(hashed, plainText.toCharArray());
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.ARGON2;
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
