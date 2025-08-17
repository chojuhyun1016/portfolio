package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.List;

/**
 * AWS 자동 모드
 * - secrets.enabled=true AND aws.secrets-manager.enabled=true 일 때만 활성화
 * - AWS SDK가 클래스패스에 존재해야 함
 */
@Configuration
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnProperty(name = {"secrets.enabled", "aws.secrets-manager.enabled"}, havingValue = "true")
@ConditionalOnClass(SecretsManagerClient.class)
public class SecretsAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public SecretsKeyResolver secretsKeyResolver() {
        return new SecretsKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretsManagerClient secretsManagerClient(SecretsManagerProperties props) {
        return SecretsManagerClient.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretsLoader secretsLoader(
            SecretsManagerProperties properties,
            SecretsKeyResolver secretsKeyResolver,
            SecretsManagerClient secretsManagerClient,
            List<SecretKeyRefreshListener> refreshListeners // 빈이 없으면 빈 리스트
    ) {
        return new SecretsLoader(properties, secretsKeyResolver, secretsManagerClient, refreshListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretsKeyClient secretsKeyClient(SecretsKeyResolver resolver) {
        return new SecretsKeyClient(resolver);
    }
}
