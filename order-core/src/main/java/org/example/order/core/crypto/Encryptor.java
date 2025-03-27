package org.example.order.core.crypto;

public interface Encryptor {
    String encrypt(String plainText);
    String decrypt(String base64CipherText);
    void setKeyFromBase64(String base64Key);
    boolean isReady();
}
