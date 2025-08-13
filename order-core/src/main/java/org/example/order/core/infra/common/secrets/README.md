# 🔑 Secrets 키 관리 모듈 (Core + AWS)

Spring Boot에서 **암호화/서명 키**를 안전하게 관리하기 위한 경량 모듈입니다.  
필요 시 **수동(Core)** 또는 **AWS Secrets Manager 자동** 모드로 전환 가능하며,  
JWT/암호화 모듈 등 다른 컴포넌트가 **동일한 인터페이스(SecretsKeyClient)** 로 키를 조회·갱신할 수 있도록 설계되었습니다.

---

## 1) 구성 개요

| 클래스/인터페이스 | 설명 |
| --- | --- |
| `AwsSecretsManagerProperties` | `aws.secrets-manager.*` 설정 프로퍼티 매핑 |
| `SecretsManualConfig` | `secrets.enabled=true` 일 때 수동(Core) 모드 활성화 |
| `SecretsAutoConfig` | `secrets.enabled=true` **그리고** `aws.secrets-manager.enabled=true` 일 때 AWS 자동 모드 활성화 |
| `SecretsKeyResolver` | 현재/백업 키 관리 (핫스왑 + 롤백) |
| `SecretsKeyClient` | 애플리케이션 코드에서 손쉽게 키 set/get |
| `SecretsLoader` | AWS Secrets Manager에서 JSON 키셋을 초기/주기 로드 |
| `SecretKeyRefreshListener` | 키 갱신 이벤트 수신 훅 |
| `CryptoKeySpec` | `{algorithm, keySize, value(Base64)}` 포맷의 키 스펙 |

> 라이브러리 클래스에는 **@Component 금지**.  
> 모든 빈 등록은 **설정( auto-config )** 에서 **조건부**로만 이루어져,  
> 모듈 임포트만으로 멋대로 올라가는 부작용을 막습니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)
아무 설정도 하지 않으면 **어떤 빈도 등록되지 않음** → 다른 모듈에 영향 없음.

### 2.2 수동(Core) 모드
`application.yml`
```yaml
secrets:
  enabled: true
```
- 등록 빈
    - `SecretsKeyResolver`
    - `SecretsKeyClient`
- **초기 키 주입은 애플리케이션 코드에서 직접 수행**
- 로컬/개발 환경에 적합

### 2.3 AWS 자동 모드
`application.yml`
```yaml
secrets:
  enabled: true

aws:
  secrets-manager:
    enabled: true
    region: ap-northeast-2
    secret-name: myapp/secret-keyset
    refresh-interval-millis: 300000  # 5분
    fail-fast: true
```
- 등록 빈
    - `SecretsKeyResolver`
    - `SecretsManagerClient`
    - `SecretsLoader` (초기 로드 + 주기 리프레시)
    - `SecretsKeyClient`
- 운영 환경에 적합, **키 실시간 갱신** 가능

---

## 3) AWS Secrets JSON 예시

AWS Secrets Manager에 아래와 같이 **하나의 JSON 객체** 로 저장합니다.
```json
{
  "JWT_SIGNING": { "algorithm": "HMAC-SHA256", "keySize": 256, "value": "BASE64_ENCODED_HMAC_KEY==" },
  "AES256":      { "algorithm": "AES",          "keySize": 256, "value": "BASE64_ENCODED_AES_KEY==" }
}
```
- `keySize` 단위: **bits** (예: 256)
- `value`: **Base64 인코딩** (URL-safe 아님)
- 필요한 키만 자유롭게 확장 가능 (예: `AES128`, `RSA_SIGNING`, ...)

---

## 4) 빠른 시작

### 4.1 수동(Core) 모드에서 초기 키 주입
```java
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;

@Bean
ApplicationRunner seedKeys(SecretsKeyClient client) {
    return args -> {
        CryptoKeySpec hmac = new CryptoKeySpec();
        hmac.setAlgorithm("HMAC-SHA256");
        hmac.setKeySize(256);
        hmac.setValue("BASE64_ENCODED_KEY=="); // 로컬 전용 테스트 키
        client.setKey("JWT_SIGNING", hmac);

        CryptoKeySpec aes = new CryptoKeySpec();
        aes.setAlgorithm("AES");
        aes.setKeySize(256);
        aes.setValue("BASE64_ENCODED_AES_KEY==");
        client.setKey("AES256", aes);
    };
}
```
> `ApplicationRunner` 대신 `CommandLineRunner` 또는 `@PostConstruct` 를 사용해도 됩니다.  
> (테스트/로컬에서만 사용하고, 운영에서는 Secrets Manager 사용을 권장)

### 4.2 AWS 자동 모드
- 설정만 켜면 `SecretsLoader` 가 **초기/주기** 로 Secrets Manager에서 키를 가져와 `SecretsKeyResolver` 에 주입합니다.
- **별도의 초기화 코드 불필요**

---

## 5) 애플리케이션에서 사용

### 5.1 서비스/컴포넌트에서 단순 조회
```java
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;

@Component
@RequiredArgsConstructor
public class CryptoService {

    private final SecretsKeyClient secrets;

    public byte[] currentJwtKey() {
        return secrets.getKey("JWT_SIGNING");
    }

    public byte[] backupJwtKeyOrNull() {
        return secrets.getBackupKey("JWT_SIGNING"); // 없으면 null
    }
}
```

### 5.2 JWT KeyResolver/KidProvider로 연결
```java
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;

@Bean
TokenProvider.KeyResolver jwtKeyResolver(org.example.order.core.infra.common.secrets.client.SecretsKeyClient secrets) {
    return token -> {
        byte[] keyBytes = secrets.getKey("JWT_SIGNING");
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    };
}

@Bean
TokenProvider.KidProvider jwtKidProvider() {
    return () -> "JWT_SIGNING"; // kid를 고정/버전 업 전략 등으로 활용 가능
}
```

---

## 6) 테스트 가이드

### 6.1 수동 모드 (가장 간단)
`application.yml`
```yaml
secrets:
  enabled: true
```
테스트에서 `ApplicationRunner` 로 원하는 키를 심고, 실제 코드와 동일하게 `SecretsKeyClient` 로 조회합니다.

### 6.2 AWS 모의 테스트
- `SecretsManagerClient` 를 **Mockito** 로 모킹하여 `GetSecretValueResponse.secretString(...)` 에 JSON 문자열을 주입합니다.
- `SecretsLoader.refreshSecrets()` 를 직접 호출하여 로드 과정을 검증합니다.

---

## 7) 보안 권장사항

- **키(원문/Base64 포함) 로그/출력 금지**
- 로컬/테스트 키는 **절대 VCS에 커밋 금지**
- AWS IAM은 해당 Secret에만 최소 권한(least privilege) 부여
- **정기 로테이션** 시 백업키 동작 확인 후, 적절한 시점에 백업 제거

---

## 8) 에러/예외 메시지

- `IllegalStateException: Secret key for [<name>] is not loaded yet.`  
  → 아직 키가 로드되지 않았습니다. 수동 모드라면 초기 주입 코드를 확인하고, 자동 모드라면 Secrets 설정/권한/네트워크를 확인하세요.

- `Invalid key size for [<name>]: expected <bytes> bytes, got <n>`  
  → `keySize`(bits) 와 실제 Base64 디코드 결과 길이(bytes)가 일치하지 않습니다. 저장된 JSON을 점검하세요.

- `Secret loading failed. Aborting app startup.` (fail-fast=true)  
  → 자동 모드에서 초기 로드 실패 시 애플리케이션 부팅을 중단합니다. 운영에서 의도된 동작입니다.

---

## 9) 설정 레퍼런스

### 9.1 수동(Core)
```yaml
secrets:
  enabled: true
```

### 9.2 AWS 자동
```yaml
secrets:
  enabled: true

aws:
  secrets-manager:
    enabled: true
    region: ap-northeast-2
    secret-name: myapp/secret-keyset
    refresh-interval-millis: 300000
    fail-fast: true
```

---

## 10) 설계 원칙 (프레임워크 친화)

- 기본은 **OFF**
- 필수 의존 빈은 **있을 때만 등록**
- 대역/NULL-Object 제공 또는 **아예 미등록**
- 라이브러리에는 **@Component 금지**
- 테스트/로컬에서는 **전역 차단 가능(설정으로 OFF)**

---

## 11) 클래스 다이어그램 (개념)

```
SecretsAutoConfig ─┬─> SecretsManagerClient
                   ├─> SecretsLoader ──> SecretsKeyResolver
                   └─> SecretsKeyClient ──> (애플리케이션 코드)

SecretsManualConfig ──> SecretsKeyResolver
                     └─> SecretsKeyClient ──> (애플리케이션 코드)
```

---

## 12) FAQ

**Q1. 수동/자동 모드를 동시에 켤 수 있나요?**  
A. 설정상 둘 다 true 여도 자동 모드가 키를 채우고, 수동으로도 추가 set이 가능합니다. 운영에서는 자동만 켜는 것을 권장합니다.

**Q2. kid 값은 꼭 써야 하나요?**  
A. 필수는 아니지만 **키 버저닝/로테이션** 식별에 유용합니다. JWT 헤더 `kid` 로 내려 다른 서비스가 올바른 키로 검증할 수 있게 합니다.

**Q3. SecretsManager JSON 스키마를 바꿔도 되나요?**  
A. 네, 내부 파서만 맞추면 됩니다. 단, `keySize`(bits) 와 `value`(Base64) 규칙은 유지하세요.

---

## 13) 샘플 코드 모음

### 13.1 수동 모드 키 주입
```java
@Bean
ApplicationRunner seedKeys(SecretsKeyClient client) {
    return args -> {
        CryptoKeySpec hmac = new CryptoKeySpec();
        hmac.setAlgorithm("HMAC-SHA256");
        hmac.setKeySize(256);
        hmac.setValue("BASE64_ENCODED_KEY==");
        client.setKey("JWT_SIGNING", hmac);
    };
}
```

### 13.2 AWS Secrets JSON 예시
```json
{
  "JWT_SIGNING": { "algorithm": "HMAC-SHA256", "keySize": 256, "value": "BASE64_ENCODED_HMAC_KEY==" },
  "AES256":      { "algorithm": "AES",          "keySize": 256, "value": "BASE64_ENCODED_AES_KEY==" }
}
```

### 13.3 JWT KeyResolver 연결
```java
@Bean
TokenProvider.KeyResolver jwtKeyResolver(SecretsKeyClient secrets) {
    return token -> new javax.crypto.spec.SecretKeySpec(secrets.getKey("JWT_SIGNING"), "HmacSHA256");
}

@Bean
TokenProvider.KidProvider jwtKidProvider() {
    return () -> "JWT_SIGNING";
}
```

---

## 14) 마지막 한 줄 요약

**필요할 때만 켜지고, 켜지면 어디서나 동일하게 쓰는 키 관리 모듈.**  
`SecretsKeyClient` 하나로 **수동/자동** 모드를 가리지 않고 키를 다룹니다.
