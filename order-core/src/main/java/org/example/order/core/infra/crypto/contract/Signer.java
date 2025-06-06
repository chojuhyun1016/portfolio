package org.example.order.core.infra.crypto.contract;

public interface Signer extends CryptoProvider {
    String sign(String message);
    boolean verify(String message, String signature);
    void setKey(String base64Key);
}
