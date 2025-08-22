package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.config.SecretsAutoConfig;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

/**
 * SecretsAutoConfig 테스트
 * - AWS SDK 클라이언트 모킹으로 SecretsLoader 동작 검증 (@PostConstruct 로 1회 로드)
 * - 리스너 콜백 호출 확인
 */
class SecretsAutoConfigTest {

    @Test
    void aws_auto_mode_loads_and_notifies_listener() throws Exception {
        // 1) 시크릿 JSON 구성 (표준 Base64)
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

        // 2) AWS Client 모킹
        SecretsManagerClient mockClient = Mockito.mock(SecretsManagerClient.class);
        Mockito.when(mockClient.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());

        AtomicBoolean notified = new AtomicBoolean(false);

        new ApplicationContextRunner()
                .withPropertyValues(
                        "secrets.enabled=true",
                        "aws.secrets-manager.enabled=true",
                        "aws.secrets-manager.region=ap-northeast-2",
                        "aws.secrets-manager.secret-name=myapp/secret-keyset",
                        "aws.secrets-manager.fail-fast=false"
                )
                // 모킹 빈 직접 주입
                .withBean(SecretsManagerClient.class, () -> mockClient)
                // ✅ Functional interface에 맞는 람다로 교체 (onSecretKeyRefreshed 호출 시 true로 세팅)
                .withBean(SecretKeyRefreshListener.class, () -> () -> notified.set(true))
                .withConfiguration(UserConfigurations.of(SecretsAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SecretsKeyResolver.class);
                    SecretsKeyResolver resolver = ctx.getBean(SecretsKeyResolver.class);

                    assertThat(resolver.getCurrentKey("aes128")).hasSize(16);
                    assertThat(resolver.getCurrentKey("aesgcm")).hasSize(32);
                    assertThat(resolver.getCurrentKey("hmac")).hasSize(32);

                    assertThat(notified.get()).isTrue(); // 리스너 호출 확인
                });
    }
}
