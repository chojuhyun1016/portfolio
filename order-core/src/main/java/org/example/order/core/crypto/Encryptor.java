package org.example.order.core.crypto;

import org.example.order.core.crypto.Impl.AesGcmEncryptor;

public interface Encryptor {
    String encrypt(String plainText);
    String decrypt(String base64Text);
    String hash(String input, String algorithm);
    void setKeyFromBase64(String base64Key);
    boolean isEncryptorReady();
    String generateRandomKeyBase64();
}
