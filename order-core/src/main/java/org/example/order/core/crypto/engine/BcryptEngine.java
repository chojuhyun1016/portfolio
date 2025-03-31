package org.example.order.core.crypto.engine;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class BcryptEngine {

    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    public static boolean verify(String plainText, String hashed) {
        return BCrypt.checkpw(plainText, hashed);
    }
}
