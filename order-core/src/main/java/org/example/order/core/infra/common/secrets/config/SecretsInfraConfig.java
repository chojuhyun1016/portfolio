package org.example.order.core.infra.common.secrets.config;

import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.config.properties.SecretsManagerProperties;
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
 * [요약]
 * - aws.secrets-manager.enabled=true 일 때만 활성화되는 시크릿 인프라 구성.
 * - AWS SDK 클라이언트, 로더, 리졸버, 클라이언트 래퍼를 빈으로 제공.
 * - 스케줄러는 옵션(주기 갱신), LocalStack일 때 부트스트랩 허용.
 * <p>
 * [핵심 포인트]
 * - 운영선 자동 최신 반영을 피하려면 Initializer 쪽에서 allowLatest=false로 선택 정책만 갱신.
 * - 본 구성파일은 부팅/로딩 파이프라인만 담당하고, 실제 키 선택은 Resolver/Initializer가 담당.
 */
@Configuration(proxyBeanMethods = false)
@Import({SecretsInfraConfig.Core.class, SecretsInfraConfig.AwsLoader.class})
public class SecretsInfraConfig {

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    @ConditionalOnClass(SecretsManagerClient.class)
    public static class AwsLoader {

        @Bean
        @ConditionalOnMissingBean
        public SecretsManagerClient secretsManagerClient(SecretsManagerProperties props) {
            String regionStr = Optional.ofNullable(props.getRegion())
                    .filter(s -> !s.isBlank()).orElse("ap-northeast-2");
            SecretsManagerClientBuilder b = SecretsManagerClient.builder().region(Region.of(regionStr));

            // LocalStack 등 엔드포인트 오버라이드
            if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
                b = b.endpointOverride(URI.create(props.getEndpoint()));
            }

            // 정적 크리덴셜(로컬/테스트) vs 기본 프로바이더(운영)
            if (props.getCredential() != null && props.getCredential().isEnabled()) {
                b = b.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getCredential().getAccessKey(), props.getCredential().getSecretKey())));
            } else {
                b = b.credentialsProvider(DefaultCredentialsProvider.create());
            }

            return b.build();
        }

        @Bean
        @ConditionalOnProperty(name = "aws.secrets-manager.scheduler-enabled", havingValue = "true")
        @ConditionalOnMissingBean(TaskScheduler.class)
        public TaskScheduler secretsTaskScheduler() {
            ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
            s.setPoolSize(1);
            s.setThreadNamePrefix("secrets-");
            s.initialize();

            return s;
        }

        @Bean
        @ConditionalOnMissingBean
        public SecretsLoader secretsLoader(
                SecretsManagerProperties props,
                SecretsKeyResolver resolver,
                SecretsManagerClient client,
                List<SecretKeyRefreshListener> listeners,
                ObjectProvider<TaskScheduler> scheduler
        ) {
            return new SecretsLoader(props, resolver, client, listeners, scheduler.getIfAvailable());
        }
    }
}
