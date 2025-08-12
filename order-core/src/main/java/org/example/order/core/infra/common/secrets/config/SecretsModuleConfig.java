package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.aws.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.List;

/**
 * 수동 임포트/조건부 자동구성용 시크릿 모듈 설정.
 * ▶ 기본 OFF : aws.secrets-manager.enabled=true 일 때만 빈 등록
 * (테스트/로컬에서는 아무것도 설정하지 않으면 자동으로 비활성)
 * <p>
 * 필요 시 @Import(SecretsModuleConfig.class) 로 명시적 주입 가능.
 */
@Configuration
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
public class SecretsModuleConfig {

    @Bean
    public SecretsManagerClient secretsManagerClient(SecretsManagerProperties props) {
        return SecretsManagerClient.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }

    @Bean
    public SecretsKeyResolver secretsKeyResolver() {
        return new SecretsKeyResolver();
    }

    @Bean
    public SecretsLoader secretsLoader(
            SecretsManagerProperties properties,
            SecretsKeyResolver secretsKeyResolver,
            SecretsManagerClient secretsManagerClient,
            List<SecretKeyRefreshListener> refreshListeners // 빈이 없으면 빈 리스트로 주입
    ) {
        return new SecretsLoader(properties, secretsKeyResolver, secretsManagerClient, refreshListeners);
    }
}
