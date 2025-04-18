package org.example.order.core.crypto.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.crypto.Hasher;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.example.order.core.crypto.engine.Argon2Engine;
import org.springframework.stereotype.Component;

@Slf4j
@Component("argon2Hasher")
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
