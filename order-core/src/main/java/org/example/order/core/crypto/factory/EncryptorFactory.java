package org.example.order.core.crypto.factory;

import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.EncryptorType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class EncryptorFactory {

    private final Map<EncryptorType, Encryptor> encryptorMap = new EnumMap<>(EncryptorType.class);

    public EncryptorFactory(
            @Qualifier("aes128Encryptor") Encryptor aes128Encryptor,
            @Qualifier("aes256Encryptor") Encryptor aes256Encryptor,
            @Qualifier("aesGcmEncryptor") Encryptor aesGcmEncryptor
    ) {
        encryptorMap.put(EncryptorType.AES128, aes128Encryptor);
        encryptorMap.put(EncryptorType.AES256, aes256Encryptor);
        encryptorMap.put(EncryptorType.AESGCM, aesGcmEncryptor);
    }

    public Encryptor getEncryptor(EncryptorType type) {
        Encryptor encryptor = encryptorMap.get(type);

        if (encryptor == null) {
            throw new IllegalArgumentException("Unsupported EncryptorType: " + type);
        }

        return encryptor;
    }
}
