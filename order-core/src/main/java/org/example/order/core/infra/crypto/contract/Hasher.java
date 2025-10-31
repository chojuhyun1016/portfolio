package org.example.order.core.infra.crypto.contract;

public interface Hasher extends CryptoProvider {
    String hash(String plainText);

    boolean matches(String plainText, String hashed);
}
