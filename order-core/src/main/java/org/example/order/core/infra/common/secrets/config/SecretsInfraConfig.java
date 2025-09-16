package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Secrets 인프라 통합 구성
 * - aws.secrets-manager.enabled=true 일 때만 활성
 * - region/endpoint/credential 은 aws 최상위에서 가져오고,
 * secrets 전용 옵션은 aws.secrets-manager.* 블록에서 읽는다.
 */
@Configuration(proxyBeanMethods = false)
@Import({SecretsInfraConfig.Core.class, SecretsInfraConfig.AwsLoader.class})
public class SecretsInfraConfig {

    /**
     * Core 레이어:
     * - 전체 게이트: aws.secrets-manager.enabled=true
     * - 프로퍼티 자동 바인딩 + Resolver/Client 등록
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    @EnableConfigurationProperties(SecretsManagerProperties.class)
    public static class Core {

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

    /**
     * AWS 로더 레이어:
     * - AWS SDK 클래스가 존재할 때만 활성
     * - Core에서 이미 enabled 조건이 걸려 있음
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    @ConditionalOnClass(SecretsManagerClient.class)
    public static class AwsLoader {

        @Bean
        @ConditionalOnMissingBean
        public SecretsManagerClient secretsManagerClient(SecretsManagerProperties props) {
            String regionStr = Optional.ofNullable(props.getRegion())
                    .filter(s -> !s.isBlank())
                    .orElse("ap-northeast-2");

            SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                    .region(Region.of(regionStr));

            if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
                builder = builder.endpointOverride(URI.create(props.getEndpoint()));
            }

            if (props.getCredential() != null && props.getCredential().isEnabled()) {
                builder = builder.credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        props.getCredential().getAccessKey(),
                                        props.getCredential().getSecretKey()
                                )
                        )
                );
            } else {
                builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            return builder.build();
        }

        /**
         * 로더 전용 스케줄러
         * - "aws.secrets-manager.scheduler-enabled=true" 일 때만 생성
         * - 이미 다른 TaskScheduler 빈이 있으면 그것을 사용, 없을 때만 1-스레드 생성
         */
        @Bean
        @ConditionalOnProperty(name = "aws.secrets-manager.scheduler-enabled", havingValue = "true")
        @ConditionalOnMissingBean(TaskScheduler.class)
        public TaskScheduler secretsTaskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("secrets-");
            scheduler.initialize();

            return scheduler;
        }

        @Bean
        @ConditionalOnMissingBean
        public SecretsLoader secretsLoader(
                SecretsManagerProperties props,
                SecretsKeyResolver secretsKeyResolver,
                SecretsManagerClient secretsManagerClient,
                List<SecretKeyRefreshListener> refreshListeners,
                ObjectProvider<TaskScheduler> taskSchedulerProvider
        ) {
            return new SecretsLoader(
                    props,
                    secretsKeyResolver,
                    secretsManagerClient,
                    refreshListeners,
                    taskSchedulerProvider.getIfAvailable()
            );
        }
    }
}
