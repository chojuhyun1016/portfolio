package org.example.order.core.crypto;

public interface Encryptor extends CryptoProvider {
    String encrypt(String plainText);
    String decrypt(String base64CipherText);
    void setKey(String base64Key);
}
