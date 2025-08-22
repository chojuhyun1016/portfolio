package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.config.SecretsAutoConfig;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.order.core.infra.common.secrets.testutil.TestKeys.std;

/**
 * 통합 테스트 (AWS 자동 모드):
 * - @SpringBootTest + 모킹 클라이언트 주입
 * - PostConstruct 초기 로드 검증
 * - refreshSecrets()로 갱신/백업/리스너 호출 횟수 검증
 */
@SpringBootTest
@Import({SecretsAutoConfig.class, SecretsAutoIT.MockBeans.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecretsAutoIT {

    static final AtomicInteger NOTIFY_COUNT = new AtomicInteger(0);
    static final SecretsManagerClient MOCK = Mockito.mock(SecretsManagerClient.class);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("secrets.enabled", () -> "true");
        r.add("aws.secrets-manager.enabled", () -> "true");
        r.add("aws.secrets-manager.region", () -> "ap-northeast-2");
        r.add("aws.secrets-manager.secret-name", () -> "myapp/secret-keyset");
        r.add("aws.secrets-manager.fail-fast", () -> "false");
    }

    @BeforeEach
    void resetCounter() {
        NOTIFY_COUNT.set(0);
    }

    static String jsonV1() throws Exception {
        Map<String, CryptoKeySpec> keys = new HashMap<>();
        CryptoKeySpec k1 = new CryptoKeySpec(); k1.setAlgorithm("AES"); k1.setKeySize(128); k1.setValue(std(16)); keys.put("aes128", k1);
        CryptoKeySpec k2 = new CryptoKeySpec(); k2.setAlgorithm("AES-GCM"); k2.setKeySize(256); k2.setValue(std(32)); keys.put("aesgcm", k2);
        CryptoKeySpec k3 = new CryptoKeySpec(); k3.setAlgorithm("HMAC-SHA256"); k3.setKeySize(256); k3.setValue(std(32)); keys.put("hmac", k3);
        return new ObjectMapper().writeValueAsString(keys);
    }

    static String jsonV2() throws Exception {
        Map<String, CryptoKeySpec> keys = new HashMap<>();
        CryptoKeySpec k1 = new CryptoKeySpec(); k1.setAlgorithm("AES"); k1.setKeySize(128); k1.setValue(std(16)); keys.put("aes128", k1);
        CryptoKeySpec k2 = new CryptoKeySpec(); k2.setAlgorithm("AES-GCM"); k2.setKeySize(256); k2.setValue(std(32)); keys.put("aesgcm", k2);
        CryptoKeySpec k3 = new CryptoKeySpec(); k3.setAlgorithm("HMAC-SHA256"); k3.setKeySize(256); k3.setValue(std(32)); keys.put("hmac", k3);
        return new ObjectMapper().writeValueAsString(keys);
    }

    @org.springframework.beans.factory.annotation.Autowired
    SecretsKeyResolver resolver;

    @org.springframework.beans.factory.annotation.Autowired
    SecretsLoader loader;

    @Test
    void postConstruct_load_and_refresh_again() throws Exception {
        // 초기 로드: MockBeans에서 V1 세팅됨
        assertThat(resolver.getCurrentKey("aes128")).hasSize(16);
        assertThat(resolver.getCurrentKey("aesgcm")).hasSize(32);
        assertThat(resolver.getCurrentKey("hmac")).hasSize(32);
        assertThat(NOTIFY_COUNT.get()).isEqualTo(1);

        // 모킹 응답을 V2로 바꾸고 refreshSecrets() 직접 호출
        Mockito.when(MOCK.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString(jsonV2()).build());

        loader.refreshSecrets();

        // 갱신 후에도 사이즈 일치 + 백업 존재 확인 (최초와 다른 값이므로 백업 생성)
        assertThat(resolver.getCurrentKey("aes128")).hasSize(16);
        assertThat(resolver.getBackupKey("aes128")).isNotNull().hasSize(16);

        assertThat(resolver.getCurrentKey("aesgcm")).hasSize(32);
        assertThat(resolver.getBackupKey("aesgcm")).isNotNull().hasSize(32);

        assertThat(resolver.getCurrentKey("hmac")).hasSize(32);
        assertThat(resolver.getBackupKey("hmac")).isNotNull().hasSize(32);

        // 리스너 2회 호출 (init 1 + refresh 1)
        assertThat(NOTIFY_COUNT.get()).isEqualTo(2);
    }

    /** 테스트용 모킹 빈 구성 */
    static class MockBeans {
        @Bean
        @Primary
        SecretsManagerClient secretsManagerClient() throws Exception {
            Mockito.when(MOCK.getSecretValue(Mockito.any(GetSecretValueRequest.class)))
                    .thenReturn(GetSecretValueResponse.builder().secretString(jsonV1()).build());
            return MOCK;
        }

        @Bean
        SecretKeyRefreshListener testRefreshListener() {
            return NOTIFY_COUNT::incrementAndGet;
        }
    }
}
