# 🔐 Secrets 모듈 테스트 가이드 (README)

---

## 📌 무엇을 테스트하나요?

아래 4가지를 **단위/경량 통합** 수준에서 검증합니다.

1) **수동 모드**: `secrets.enabled=true` 만 켠 상태에서, 사용자가 코드로 키를 `set/get` 할 수 있는지 (`SecretsManualConfig` 로딩)
2) **자동 모드**: `secrets.enabled=true` + `aws.secrets-manager.enabled=true` 일 때, AWS에서 키를 읽어와 자동 반영되는지 (`SecretsAutoConfig` 로딩)
3) **로더 동작**: `SecretsLoader` 가 AWS SDK를 통해 시크릿 JSON을 읽고 `SecretsKeyResolver` 에 반영한 뒤, 리스너를 알리는지
4) **리졸버 동작**: `SecretsKeyResolver` 가 키 업데이트 시 **백업 키(롤백용)** 를 유지하고, **미등록 키 접근 시 예외**를 던지는지

---

## 🧩 사용 기술

- **ApplicationContextRunner**  
  최소한의 빈만 올려서 조건부 자동 구성(`@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`)을 안전하게 검증합니다.  
  → **메인 클래스, 전체 Spring 컨텍스트 불필요**

- **Mockito**
    - `SecretsManagerClient`(AWS SDK) **모킹**
    - AWS 메서드 오버로드 모호성 해결: `when(client.getSecretValue(ArgumentMatchers.<GetSecretValueRequest>any()))...` 형태로 **제네릭 타입 명시**
    - `ArgumentCaptor<GetSecretValueRequest>` 로 **호출 파라미터(secretId)** 캡처/검증

- **AssertJ / JUnit5**
    - 컨텍스트 내 **빈 존재/부재** 검증
    - 배열/예외 메시지/상태 검사

---

## 🧪 테스트 코드 전체 (설명 포함)

아래 코드는 그대로 붙여넣어 사용할 수 있습니다. (패키지 경로는 환경에 맞게 조정)

```java
package org.example.order.core.infra.common.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.core.infra.common.secrets.aws.AwsSecretsManagerProperties;
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
            localSpec.setValue(Base64.getEncoder().encodeToString(
                    "local-32-byte-key-000000000000000".getBytes(StandardCharsets.UTF_8)
            ));
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
        AwsSecretsManagerProperties props = new AwsSecretsManagerProperties();
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
```

---

## ⚙️ 속성 기반 모드 제어

- **수동(Core) 모드**: `secrets.enabled=true`
    - 등록 빈: `SecretsKeyResolver`, `SecretsKeyClient`
    - 사용자가 코드에서 `SecretsKeyClient#setKey(...)` 로 키 주입 → `getKey(...)` 로 조회
    - AWS 연동 없음 (클래스패스/네트워크 불필요)

- **자동(AWS) 모드**: `secrets.enabled=true` + `aws.secrets-manager.enabled=true`
    - 등록 빈: `SecretsKeyResolver`, `SecretsKeyClient`, `SecretsManagerClient`, `SecretsLoader`
    - `SecretsLoader` 가 주기적으로 AWS Secrets Manager에서 JSON을 가져와 `Resolver` 에 갱신
    - 리스너(`SecretKeyRefreshListener`)가 있으면 키 갱신 시 콜백 호출

예시 속성(테스트에서 사용한 값과 동일):

```properties
# Core(수동)만 켜기
secrets.enabled=true

# Auto(AWS) 모드 켜기
aws.secrets-manager.enabled=true
aws.secrets-manager.region=ap-northeast-2
aws.secrets-manager.secret-name=myapp/secret-keyset
```

---

## 🧾 AWS Secrets JSON 포맷

`SecretsLoader` 는 아래와 같은 JSON 구조를 기대합니다.  
키 이름(`JWT_SIGNING`)별로 `CryptoKeySpec` 를 매핑합니다.

```json
{
  "JWT_SIGNING": {
    "algorithm": "HMAC-SHA256",
    "keySize": 256,
    "value": "BASE64_ENCODED_KEY=="
  },
  "ANOTHER_KEY": {
    "algorithm": "AES",
    "keySize": 128,
    "value": "BASE64_ENCODED_KEY=="
  }
}
```

- `value` 는 **Base64 인코딩된 키 바이트**입니다.
- `keySize`(비트)와 `value` 실제 바이트 길이(`keySize/8`)가 **일치**해야 합니다.  
  일치하지 않으면 `SecretsLoader` 가 `IllegalArgumentException` 을 던집니다(방어적 검증).

---

## 🪄 Mockito 팁 (AWS 오버로드 문제)

AWS SDK v2 의 `getSecretValue` 는 **오버로드**가 있어 Mockito가 혼동할 수 있습니다.  
아래처럼 **제네릭 타입 파라미터**를 명시해서 안전하게 모킹하세요.

```java
when(mockClient.getSecretValue(ArgumentMatchers.<GetSecretValueRequest>any()))
    .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());
```

또는 캡처해서 **호출 파라미터 검증**:

```java
ArgumentCaptor<GetSecretValueRequest> captor = ArgumentCaptor.forClass(GetSecretValueRequest.class);
verify(mockClient).getSecretValue(captor.capture());
assertEquals("dummySecret", captor.getValue().secretId());
```

---

## 🚀 실행 방법

Gradle 기준:

```bash
./gradlew :order-core:test --tests "*SecretsModuleTest*"
```

또는 모듈 전체 테스트:

```bash
./gradlew :order-core:test
```

> **TIP**: `ApplicationContextRunner` 를 사용하므로, **메인 클래스가 없어도** 빠르게 테스트 가능합니다.

---

## ✅ 요약 체크리스트

- [x] 수동 모드: `secrets.enabled=true` → 키를 코드에서 `set/get`
- [x] 자동 모드: `secrets.enabled=true` + `aws.secrets-manager.enabled=true` → AWS에서 자동 로드
- [x] 오버로드 모호성: `ArgumentMatchers.<GetSecretValueRequest>any()` 로 해결
- [x] 파라미터 검증: `ArgumentCaptor<GetSecretValueRequest>` 로 `secretId` 확인
- [x] 리스너 호출: 갱신 시 `onSecretKeyRefreshed()` 한번 이상 호출

---

## 🧠 설계 의도 (테스트 관점)

- **라이브러리(@Component 금지) + 조건부 구성**: 사용자가 **원할 때만** 켜지도록
- **핫스왑/롤백 가능**: 운영 중 키 교체를 안전하게
- **단일 인터페이스(SecretsKeyClient)**: 수동/자동 모드 모두에서 **동일한 사용성** 제공
- **빠른 테스트**: AWS/DB/Redis 없이, **Mockito + ContextRunner** 만으로 핵심 시나리오 커버

필요 시, 위 테스트를 기반으로 다른 모듈(예: JWT 키 주입/조회, 암·복호화 키 교체)에서도 동일한 패턴으로 손쉽게 확장할 수 있습니다.
