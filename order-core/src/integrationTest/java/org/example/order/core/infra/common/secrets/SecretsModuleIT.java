package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.config.SecretsInfraConfig;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

/**
 * 시크릿 모듈 통합 테스트
 * SecretsCoreIT, SecretsInfraIT, SecretsInfraConfigTest를 통합한 클래스
 * 목적
 * - 클라이언트 set, get, backup 동작 검증
 * - AWS 자동 모드에서 초기 로드와 리스너 알림 검증
 * - 전역 스케줄러는 활성화하지 않고 scheduler-enabled 설정에 따라 동작 확인
 */
@SpringBootTest(classes = SecretsModuleIT.Boot.class)
@Import({SecretsInfraConfig.class, SecretsModuleIT.MockBeans.class})
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecretsModuleIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot {
    }

    /**
     * SpringBootTest 컨텍스트 전역 속성 설정
     * aws.secrets-manager.enabled 가 true여야 관련 빈이 생성된다
     * 실제 AWS 호출은 하지 않도록 Primary mock을 주입한다
     * fail-fast는 false로 두고 scheduler-enabled도 false로 두어 초기 1회 로드만 검증한다
     */
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("aws.secrets-manager.enabled", () -> "true");
        r.add("aws.secrets-manager.region", () -> "ap-northeast-2");
        r.add("aws.secrets-manager.secret-name", () -> "dummy/secret-keyset");
        r.add("aws.secrets-manager.fail-fast", () -> "false");
        r.add("aws.secrets-manager.scheduler-enabled", () -> "false");
        r.add("spring.task.scheduling.enabled", () -> "false");
    }

    /**
     * SpringBootTest 컨텍스트에서 사용되는 기본 mock 빈
     * AWS와의 실제 상호작용을 막기 위해 SecretsManagerClient를 mock으로 등록한다
     */
    static class MockBeans {
        @Bean
        @Primary
        SecretsManagerClient secretsManagerClientMock() {
            return Mockito.mock(SecretsManagerClient.class);
        }
    }

    // 테스트 1 클라이언트 set, get, backup 동작 검증
    @org.springframework.beans.factory.annotation.Autowired
    SecretsKeyClient client;

    @Test
    void set_and_update_then_backup_exists() {
        CryptoKeySpec spec1 = new CryptoKeySpec();
        spec1.setAlgorithm("AES");
        spec1.setKeySize(128);
        spec1.setValue(std(16));

        client.setKey("aes128", spec1);
        assertThat(client.getKey("aes128")).hasSize(16);
        assertThat(client.getBackupKey("aes128")).isNull();

        CryptoKeySpec spec2 = new CryptoKeySpec();
        spec2.setAlgorithm("AES");
        spec2.setKeySize(128);
        spec2.setValue(std(16));

        client.setKey("aes128", spec2);
        assertThat(client.getKey("aes128")).hasSize(16);
        assertThat(client.getBackupKey("aes128")).isNotNull().hasSize(16);
    }

    // 테스트 2 AWS 자동 모드 초기 로드와 리스너 알림 검증
    @BeforeEach
    void beforeEach() {
        // 필요시 각 테스트 전에 초기화 로직 추가 가능
    }

    @Test
    void aws_auto_mode_initial_load_and_listener_notification() throws Exception {
        // 3개의 키를 가진 시크릿 JSON 생성
        Map<String, CryptoKeySpec> keys = new HashMap<>();
        CryptoKeySpec k1 = new CryptoKeySpec();
        k1.setAlgorithm("AES");
        k1.setKeySize(128);
        k1.setValue(std(16));
        keys.put("aes128", k1);

        CryptoKeySpec k2 = new CryptoKeySpec();
        k2.setAlgorithm("AES-GCM");
        k2.setKeySize(256);
        k2.setValue(std(32));
        keys.put("aesgcm", k2);

        CryptoKeySpec k3 = new CryptoKeySpec();
        k3.setAlgorithm("HMAC-SHA256");
        k3.setKeySize(256);
        k3.setValue(std(32));
        keys.put("hmac", k3);

        String secretJson = new ObjectMapper().writeValueAsString(keys);

        // 테스트 전용 mock 클라이언트 생성
        SecretsManagerClient mockClient = Mockito.mock(SecretsManagerClient.class);
        Mockito.when(mockClient.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());

        AtomicBoolean notified = new AtomicBoolean(false);

        // ApplicationContextRunner로 격리된 테스트 실행
        new ApplicationContextRunner()
                .withPropertyValues(
                        "aws.secrets-manager.enabled=true",
                        "aws.secrets-manager.region=ap-northeast-2",
                        "aws.secrets-manager.secret-name=myapp/secret-keyset",
                        "aws.secrets-manager.fail-fast=false",
                        "aws.secrets-manager.scheduler-enabled=false",
                        "spring.task.scheduling.enabled=false"
                )
                .withBean(SecretsManagerClient.class, () -> mockClient)
                .withBean(SecretKeyRefreshListener.class, () -> () -> notified.set(true))
                .withConfiguration(UserConfigurations.of(SecretsInfraConfig.class))
                .run(ctx -> {
                    SecretsLoader loader = ctx.getBean(SecretsLoader.class);

                    // ApplicationReadyEvent 시점에 초기 1회 로드를 트리거한다
                    loader.onApplicationReady();

                    assertThat(ctx).hasSingleBean(SecretsKeyResolver.class);
                    SecretsKeyResolver resolver = ctx.getBean(SecretsKeyResolver.class);

                    // 키들이 정상적으로 로드되었는지 검증
                    assertThat(resolver.getCurrentKey("aes128")).hasSize(16);
                    assertThat(resolver.getCurrentKey("aesgcm")).hasSize(32);
                    assertThat(resolver.getCurrentKey("hmac")).hasSize(32);

                    // 리스너가 호출되었는지 검증
                    assertThat(notified.get()).isTrue();
                });
    }
}
