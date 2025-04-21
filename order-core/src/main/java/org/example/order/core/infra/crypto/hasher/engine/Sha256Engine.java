package org.example.order.core.infra.crypto.hasher.engine;

import java.security.MessageDigest;

public class Sha256Engine {

    public static byte[] hash(byte[] input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        return digest.digest(input);
    }
}
