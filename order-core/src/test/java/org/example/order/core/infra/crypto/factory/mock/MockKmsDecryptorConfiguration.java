package org.example.order.core.infra.crypto.factory.mock;

import org.example.order.core.infra.common.kms.config.KmsProperties;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Base64;

@TestConfiguration
public class MockKmsDecryptorConfiguration {

    @Bean
    public EncryptProperties encryptProperties() {
        EncryptProperties properties = new EncryptProperties();
        properties.getAes128().setKey(generateKey(16));
        properties.getAes256().setKey(generateKey(32));
        properties.getAesgcm().setKey(generateKey(32));
        return properties;
    }

    @Bean
    public KmsDecryptor kmsDecryptor(KmsProperties kmsProperties) {
        return new TestKmsDecryptor(kmsProperties);
    }

    private static String generateKey(int length) {
        byte[] keyBytes = new byte[length];
        for (int i = 0; i < length; i++) keyBytes[i] = (byte) (i + 1);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}
