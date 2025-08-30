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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.List;

/**
 * Secrets 인프라 통합 구성 (AWS 모드 전용)
 * - aws.secrets-manager.enabled=true 일 때만 전체 블록 활성화(수동 모드 없음)
 * - Core Bean(Resolver, Client) + AWS SDK 존재 시 로더(SecretsLoader) 조립
 * <p>
 * 변경점:
 * - 전역 @EnableScheduling 제거(오염 방지)
 * - 스케줄러(TaskScheduler)는 "aws.secrets-manager.scheduler-enabled=true" 일 때만,
 * 그리고 MissingBean 인 경우에만 1스레드 스케줄러를 생성
 * - 스케줄러가 없으면 초기 1회 로드도 수행하지 않음(요구사항 반영)
 */
@Configuration(proxyBeanMethods = false)
@Import({SecretsInfraConfig.Core.class, SecretsInfraConfig.AwsLoader.class})
public class SecretsInfraConfig {

    /**
     * Core 레이어:
     * - 전체 게이트: aws.secrets-manager.enabled=true
     * - 프로퍼티 바인딩 + Resolver/Client 등록
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    public static class Core {

        /**
         * 명시적 프로퍼티 바인딩 Bean
         * - 중복 바인딩 방지를 위해 @EnableConfigurationProperties / 클래스 레벨 @ConfigurationProperties 미사용
         */
        @Bean
        @ConfigurationProperties(prefix = "aws.secrets-manager")
        @ConditionalOnMissingBean
        public SecretsManagerProperties secretsManagerProperties() {
            return new SecretsManagerProperties();
        }

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
     * - 외부 조건(클래스패스) 충족 시에만 활성
     * - 프로퍼티 조건은 Core에서 이미 걸려 있으므로 중복 지정하지 않음
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "aws.secrets-manager.enabled", havingValue = "true")
    @ConditionalOnClass(SecretsManagerClient.class)
    public static class AwsLoader {

        @Bean
        @ConditionalOnMissingBean
        public SecretsManagerClient secretsManagerClient(SecretsManagerProperties props) {
            return SecretsManagerClient.builder().region(Region.of(props.getRegion())).build();
        }

        /**
         * 로더 전용 스케줄러
         * - "aws.secrets-manager.scheduler-enabled=true" 로 명시적으로 켤 때만 생성
         * - 이미 다른 TaskScheduler 빈이 있으면 그것을 사용, 없을 때만 1-스레드 생성
         */
        @Bean
        @ConditionalOnProperty(name = "aws.secrets-manager.scheduler-enabled", havingValue = "true")
        @ConditionalOnMissingBean(TaskScheduler.class)
        public TaskScheduler secretsTaskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);                  // 단일 스레드
            scheduler.setThreadNamePrefix("secrets-"); // 디버깅 편의
            scheduler.initialize();

            return scheduler;
        }

        @Bean
        @ConditionalOnMissingBean
        public SecretsLoader secretsLoader(SecretsManagerProperties properties, SecretsKeyResolver secretsKeyResolver, SecretsManagerClient secretsManagerClient,
                                           // 리스너 빈이 없으면 빈 리스트가 주입됨 (Spring 기본 동작)
                                           List<SecretKeyRefreshListener> refreshListeners,
                                           // 스케줄러는 선택적 주입(ObjectProvider) → 없으면 초기 로드/주기 갱신 모두 수행하지 않음
                                           ObjectProvider<TaskScheduler> taskSchedulerProvider) {
            TaskScheduler maybeScheduler = taskSchedulerProvider.getIfAvailable();

            // ★ ApplicationReadyEvent 시
            //   - properties.isSchedulerEnabled() && scheduler 존재 → 초기 1회 로드 + fixedDelay 주기
            //   - 아니면 아무 것도 하지 않음(요구사항대로 1회 로드도 스킵)
            return new SecretsLoader(properties, secretsKeyResolver, secretsManagerClient, refreshListeners, maybeScheduler // null 허용
            );
        }
    }
}
