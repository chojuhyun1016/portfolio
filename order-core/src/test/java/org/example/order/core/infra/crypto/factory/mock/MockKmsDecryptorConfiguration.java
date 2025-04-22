package org.example.order.core.infra.crypto.factory.mock;

import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.decryptor.KmsDecryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockKmsDecryptorConfiguration {

    @Bean
    public KmsDecryptor kmsDecryptor(EncryptProperties encryptProperties) {
        return new TestKmsDecryptor(encryptProperties);
    }
}
