package org.example.order.core.infra.common.secrets.client;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.common.secrets.config.SecretsManagerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * AWS Secrets Manager Client 제공
 */
@Configuration
@RequiredArgsConstructor
public class SecretsManagerClientProvider {

    private final SecretsManagerProperties properties;

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
