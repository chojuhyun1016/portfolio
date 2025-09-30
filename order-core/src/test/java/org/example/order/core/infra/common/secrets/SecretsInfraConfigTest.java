//package org.example.order.core.infra.common.secrets;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.example.order.core.infra.common.secrets.config.SecretsInfraConfig;
//import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
//import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
//import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
//import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.boot.autoconfigure.AutoConfigurations;
//import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
//import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
//import org.springframework.boot.context.annotation.UserConfigurations;
//import org.springframework.boot.test.context.runner.ApplicationContextRunner;
//import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;
//
///**
// * 구성 단위 테스트.
// * AWS 자동 모드에서 초기 로드와 리스너 알림을 검증한다.
// * 스케줄러는 끄고 수동 트리거로 한 번만 로드한다.
// * 비활성 모드에서는 관련 빈이 등록되지 않아야 한다.
// */
//class SecretsInfraConfigTest {
//
//    @Test
//    void aws_auto_mode_loads_and_notifies_listener() throws Exception {
//        Map<String, CryptoKeySpec> keys = new HashMap<>();
//
//        CryptoKeySpec k1 = new CryptoKeySpec();
//        k1.setAlgorithm("AES");
//        k1.setKeySize(128);
//        k1.setValue(std(16));
//        keys.put("aes128", k1);
//
//        CryptoKeySpec k2 = new CryptoKeySpec();
//        k2.setAlgorithm("AES-GCM");
//        k2.setKeySize(256);
//        k2.setValue(std(32));
//        keys.put("aesgcm", k2);
//
//        CryptoKeySpec k3 = new CryptoKeySpec();
//        k3.setAlgorithm("HMAC-SHA256");
//        k3.setKeySize(256);
//        k3.setValue(std(32));
//        keys.put("hmac", k3);
//
//        String secretJson = new ObjectMapper().writeValueAsString(keys);
//
//        SecretsManagerClient mockClient = Mockito.mock(SecretsManagerClient.class);
//        Mockito.when(mockClient.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
//                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());
//
//        AtomicBoolean notified = new AtomicBoolean(false);
//
//        new ApplicationContextRunner()
//                .withConfiguration(AutoConfigurations.of(
//                        ConfigurationPropertiesAutoConfiguration.class,
//                        JacksonAutoConfiguration.class
//                ))
//                .withPropertyValues(
//                        "aws.secrets-manager.enabled=true",
//                        "aws.secrets-manager.region=ap-northeast-2",
//                        "aws.secrets-manager.secret-name=myapp/secret-keyset",
//                        "aws.secrets-manager.fail-fast=true",
//                        "aws.secrets-manager.scheduler-enabled=false",
//                        "spring.task.scheduling.enabled=false"
//                )
//                .withBean(SecretsManagerClient.class, () -> mockClient)
//                .withBean(SecretKeyRefreshListener.class, () -> () -> notified.set(true))
//                .withConfiguration(UserConfigurations.of(SecretsInfraConfig.class))
//                .run(ctx -> {
//                    SecretsLoader loader = ctx.getBean(SecretsLoader.class);
//
//                    // 이벤트 없이 수동으로 1회 로드를 수행한다.
//                    loader.refreshNowForTest();
//
//                    SecretsKeyResolver resolver = ctx.getBean(SecretsKeyResolver.class);
//                    assertThat(resolver.getCurrentKey("aes128")).hasSize(16);
//                    assertThat(resolver.getCurrentKey("aesgcm")).hasSize(32);
//                    assertThat(resolver.getCurrentKey("hmac")).hasSize(32);
//
//                    assertThat(notified.get()).isTrue();
//                });
//    }
//
//    @Test
//    void disabled_mode_has_no_beans() {
//        new ApplicationContextRunner()
//                .withConfiguration(AutoConfigurations.of(
//                        ConfigurationPropertiesAutoConfiguration.class,
//                        JacksonAutoConfiguration.class
//                ))
//                .withPropertyValues(
//                        "aws.secrets-manager.enabled=false"
//                )
//                .withConfiguration(UserConfigurations.of(SecretsInfraConfig.class))
//                .run(ctx -> {
//                    assertThat(ctx).doesNotHaveBean(SecretsKeyResolver.class);
//                    assertThat(ctx).doesNotHaveBean(SecretsLoader.class);
//                    assertThat(ctx).doesNotHaveBean(SecretsManagerClient.class);
//                });
//    }
//}
