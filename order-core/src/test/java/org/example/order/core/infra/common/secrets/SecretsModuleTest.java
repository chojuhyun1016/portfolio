package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.config.SecretsAutoConfig;
import org.example.order.core.infra.common.secrets.config.SecretsManualConfig;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Secrets 모듈 단위/경량 통합 테스트 (Spring 전체 컨텍스트 불필요)
 * - 1) 수동 모드(secrets.enabled=true)에서 키 set/get 확인 (컨텍스트 러너)
 * - 2) 자동 모드(secrets.enabled=true & aws.secrets-manager.enabled=true)에서 로딩/조회 확인 (컨텍스트 러너)
 * - 3) SecretsLoader → AWS에서 읽어와 Resolver 반영 + 리스너 알림
 * - 4) SecretsKeyResolver → 백업 키 유지 + 미등록 키 접근 시 예외
 */
class SecretsModuleTest {

    @Test
    @DisplayName("3) 수동 모드(secrets.enabled=true): SecretsKeyClient로 키 set/get")
    void manual_mode_register_and_get_key_with_contextRunner() {
        // ApplicationContextRunner: 최소 컨텍스트로 조건부 빈 등록을 검증
        ApplicationContextRunner ctx = new ApplicationContextRunner()
                // 수동 모드 활성화
                .withPropertyValues("secrets.enabled=true")
                // 수동 구성만 로드
                .withConfiguration(UserConfigurations.of(SecretsManualConfig.class));

        ctx.run(context -> {
            // 빈 존재 확인
            assertThat(context).hasSingleBean(SecretsKeyResolver.class);
            assertThat(context).hasSingleBean(SecretsKeyClient.class);
            assertThat(context).doesNotHaveBean(SecretsManagerClient.class);
            assertThat(context).doesNotHaveBean(SecretsLoader.class);

            SecretsKeyClient client = context.getBean(SecretsKeyClient.class);

            // when: 코드로 키 주입 (핫스왑)
            CryptoKeySpec localSpec = new CryptoKeySpec();
            localSpec.setAlgorithm("HMAC-SHA256");
            localSpec.setKeySize(256);
            localSpec.setValue(Base64.getEncoder().encodeToString("local-32-byte-key-000000000000000".getBytes(StandardCharsets.UTF_8)));
            client.setKey("JWT_SIGNING", localSpec);

            // then: 조회 가능
            byte[] restored = client.getKey("JWT_SIGNING");
            assertThat(restored).isNotEmpty();
        });
    }

    @Test
    @DisplayName("4) 자동 모드(secrets.enabled & aws.secrets-manager.enabled): AWS 로딩 후 SecretsKeyClient로 조회")
    void auto_mode_load_from_aws_and_get_with_contextRunner() throws Exception {
        // given: AWS 시크릿 JSON
        byte[] keyBytes = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8); // 32B
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm("HMAC-SHA256");
        spec.setKeySize(256);
        spec.setValue(Base64.getEncoder().encodeToString(keyBytes));
        String secretJson = new ObjectMapper().writeValueAsString(Map.of("JWT_SIGNING", spec));

        // AWS Client Mock (빈으로 주입)
        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        when(mockClient.getSecretValue(ArgumentMatchers.<GetSecretValueRequest>any()))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());

        // 리스너 목(선택)
        SecretKeyRefreshListener listener = mock(SecretKeyRefreshListener.class);

        ApplicationContextRunner ctx = new ApplicationContextRunner()
                // 자동 모드 활성화 (두 플래그 모두 true)
                .withPropertyValues(
                        "secrets.enabled=true",
                        "aws.secrets-manager.enabled=true",
                        "aws.secrets-manager.region=ap-northeast-2",
                        "aws.secrets-manager.secret-name=myapp/secret-keyset"
                )
                // 자동 구성 로드
                .withConfiguration(UserConfigurations.of(SecretsAutoConfig.class))
                // 모킹 빈 주입
                .withBean(SecretsManagerClient.class, () -> mockClient)
                .withBean(SecretKeyRefreshListener.class, () -> listener);

        ctx.run(context -> {
            // 자동 모드 컴포넌트 확인
            assertThat(context).hasSingleBean(SecretsKeyResolver.class);
            assertThat(context).hasSingleBean(SecretsKeyClient.class);
            assertThat(context).hasSingleBean(SecretsLoader.class);
            assertThat(context).hasSingleBean(SecretsManagerClient.class);

            // when: 로더를 통해 강제 리프레시(초기 @PostConstruct도 있지만, 명시 호출로도 안전)
            SecretsLoader loader = context.getBean(SecretsLoader.class);
            loader.refreshSecrets();

            // then: 클라이언트에서 조회 가능
            SecretsKeyClient client = context.getBean(SecretsKeyClient.class);
            assertArrayEquals(keyBytes, client.getKey("JWT_SIGNING"));

            // and: 리스너 호출 확인
            verify(listener, atLeastOnce()).onSecretKeyRefreshed();
        });
    }

    @Test
    @DisplayName("1) AWS → SecretsLoader → Resolver 반영 + 리스너 알림")
    void loadFromAws_and_notifyListener() throws Exception {
        // given: 256-bit HMAC 키 (32바이트)
        byte[] hmacKey = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm("HMAC-SHA256");
        spec.setKeySize(256);
        spec.setValue(Base64.getEncoder().encodeToString(hmacKey));

        // AWS Secrets JSON 예시: {"JWT_SIGNING": {algorithm,keySize,value}}
        String secretJson = new ObjectMapper().writeValueAsString(Map.of("JWT_SIGNING", spec));

        // AWS SDK Client Mock
        SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
        // ★ 오버로드 모호성 방지: 제네릭 타입을 명시
        when(mockClient.getSecretValue(ArgumentMatchers.<GetSecretValueRequest>any()))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());

        // 프로퍼티 구성
        SecretsManagerProperties props = new SecretsManagerProperties();
        props.setRegion("ap-northeast-2");
        props.setSecretName("dummySecret");
        props.setFailFast(true);

        // Resolver + 리스너
        SecretsKeyResolver resolver = new SecretsKeyResolver();
        SecretKeyRefreshListener listener = mock(SecretKeyRefreshListener.class);

        // loader
        SecretsLoader loader = new SecretsLoader(props, resolver, mockClient, List.of(listener));

        // when: AWS에서 로드 + Resolver 반영
        loader.refreshSecrets();

        // then: 1) Resolver 반영 확인
        assertArrayEquals(hmacKey, resolver.getCurrentKey("JWT_SIGNING"));
        assertNull(resolver.getBackupKey("JWT_SIGNING"), "최초 로드는 백업이 없어야 함");

        // then: 2) AWS getSecretValue() 호출 파라미터 검증 (ArgumentCaptor 사용)
        ArgumentCaptor<GetSecretValueRequest> captor = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(mockClient).getSecretValue(captor.capture());
        assertEquals("dummySecret", captor.getValue().secretId());

        // then: 3) 리스너 알림 확인
        verify(listener, times(1)).onSecretKeyRefreshed();
        verifyNoMoreInteractions(listener);
    }

    @Test
    @DisplayName("2) Resolver 백업 유지 + 미등록 키 접근 시 예외")
    void resolver_backup_and_missingKeyErrors() {
        SecretsKeyResolver resolver = new SecretsKeyResolver();

        // given: 최초 키 등록 (백업 없음)
        CryptoKeySpec first = new CryptoKeySpec();
        first.setAlgorithm("AES");
        first.setKeySize(24); // 3 bytes * 8 bits
        first.setValue(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
        resolver.updateKey("aesKey", first);

        // when: 새 키로 업데이트 → 이전 키는 백업으로 이동
        CryptoKeySpec second = new CryptoKeySpec();
        second.setAlgorithm("AES");
        second.setKeySize(24);
        second.setValue(Base64.getEncoder().encodeToString(new byte[]{9, 9, 9}));
        resolver.updateKey("aesKey", second);

        // then: 백업/현재 키 검증
        assertArrayEquals(new byte[]{1, 2, 3}, resolver.getBackupKey("aesKey"));
        assertArrayEquals(new byte[]{9, 9, 9}, resolver.getCurrentKey("aesKey"));

        // and: 미등록 키 접근 시 예외 발생
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resolver.getCurrentKey("missingKey"));
        assertTrue(ex.getMessage().contains("missingKey"));
    }
}
