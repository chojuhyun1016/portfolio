package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CORE(수동) 모드
 * - secrets.enabled=true 일 때만 활성화
 * - 사용자가 코드로 SecretsKeyClient를 통해 키를 직접 set/get
 */
@Configuration
@ConditionalOnProperty(name = "secrets.enabled", havingValue = "true")
public class SecretsManualConfig {

    @Bean
    @ConditionalOnMissingBean
    public SecretsKeyResolver secretsKeyResolver() {
        return new SecretsKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretsKeyClient secretsKeyClient(SecretsKeyResolver resolver) {
        return new SecretsKeyClient(resolver);
    }
}
