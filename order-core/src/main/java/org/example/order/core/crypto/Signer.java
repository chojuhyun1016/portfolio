package org.example.order.core.crypto;

public interface Signer extends CryptoProvider {
    String sign(String message);
    boolean verify(String message, String signature);
}
