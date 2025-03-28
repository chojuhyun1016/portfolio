package org.example.order.core.crypto;

import org.example.order.core.crypto.code.EncryptorType;

public interface Encryptor {
    String encrypt(String plainText);
    String decrypt(String base64CipherText);
    void setKey(String base64Key);
    boolean isReady();
    EncryptorType getType();
}
