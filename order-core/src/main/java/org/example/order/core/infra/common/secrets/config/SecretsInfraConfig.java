package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.List;

/**
 * Secrets 인프라 통합 구성
 * - secrets.enabled=true 일 때만 전체 블록 활성화
 * - Core Bean(Resolver, Client) 등록
 * - aws.secrets-manager.enabled=true && AWS SDK 존재 시에만 AWS 로더(SecretsLoader) 조립
 * <p>
 * 사용법:
 *
 * @Import(SecretsInfraConfig.class) 변경점:
 * - @EnableScheduling 제거(전역 스케줄 인프라 오염 방지)
 * - AWS 로더가 켜진 경우에만 TaskScheduler 기반 fixedDelay 스케줄을 내부적으로 사용
 * - 코어만 켠 경우(aws.secrets-manager.enabled=false)에는 로더/스케줄 전혀 등록되지 않음
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "secrets.enabled", havingValue = "true")
@EnableConfigurationProperties(SecretsManagerProperties.class)
@Import(SecretsInfraConfig.AwsLoaderConfig.class) // 내부에서 조건부로만 동작
public class SecretsInfraConfig {

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

    /**
     * AWS Secrets Manager 로더 조립
     * - 외부 조건(클래스패스 + 프로퍼티)이 모두 충족될 때만 Import
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SecretsManagerClient.class)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    @Import(SecretsSchedulerConfig.class) // 로더용 TaskScheduler 제공(없을 때만)
    public static class AwsLoaderConfig {

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
                // 리스너 빈이 없으면 빈 리스트가 주입됨 (Spring 기본 동작)
                List<SecretKeyRefreshListener> refreshListeners,
                TaskScheduler taskScheduler // 기존 @Scheduled 대체(로더 켜진 경우에만 주입)
        ) {
            // ★ ApplicationReadyEvent 1회 로드 + fixedDelay 반복(작업 종료 후 지연) 스케줄
            return new SecretsLoader(properties, secretsKeyResolver, secretsManagerClient, refreshListeners, taskScheduler);
        }
    }
}
