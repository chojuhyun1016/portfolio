package org.example.order.core.crypto;

public interface Hasher extends CryptoProvider {
    String hash(String plainText);
    boolean matches(String plainText, String hashed);
}
