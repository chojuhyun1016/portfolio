# 🔐 infra:secrets — Secrets Manager/수동 키 로딩 모듈

Spring Boot에서 AES/HMAC 등 **암·복호화용 SecretKey**를 안전하게 주입/관리하기 위한 경량 모듈입니다.  
**AWS 자동 모드** 또는 **CORE 수동 모드**로 동작하며, 서비스 코드는 **SecretsKeyClient** 하나로 `setKey/getKey/getBackupKey`를 단순히 호출하면 됩니다.

---

## 1) 구성 개요

| 클래스/인터페이스                   | 설명 |
|-------------------------------------|------|
| `SecretsManualConfig`               | `secrets.enabled=true` 시 CORE(수동) 모드 활성화, Resolver/Client 빈 등록 |
| `SecretsAutoConfig`                 | `secrets.enabled=true` & `aws.secrets-manager.enabled=true` 시 AWS 자동 모드 활성화 |
| `SecretsKeyClient`                  | 서비스 코드용 얇은 래퍼: `setKey/getKey/getBackupKey` 제공 |
| `SecretsKeyResolver`                | 현재/백업 키 보관(핫스왑/롤백), 동시성 안전 |
| `SecretsLoader`                     | AWS Secrets Manager에서 주기적으로 비밀(JSON) 로드 → Resolver 반영 → 리스너 알림 |
| `SecretKeyRefreshListener`          | 로드/교체 후 콜백 인터페이스 |
| `CryptoKeySpec`                     | `{ algorithm, keySize, value(Base64) }` 키 스펙 모델 |
| `SecretsManagerProperties`          | `region`, `secret-name`, `refresh-interval-millis`, `fail-fast` 등 |

> **빈 등록 원칙**  
> 라이브러리 클래스에는 `@Component` 금지.  
> 모든 빈은 **조건부(@ConditionalOnProperty, @ConditionalOnMissingBean)** 로만 등록되어 불필요한 부작용을 방지합니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)
아무 설정도 없으면 빈이 등록되지 않으며, 다른 모듈에 영향을 주지 않습니다.

### 2.2 CORE(수동) 모드
```properties
secrets.enabled=true
```
- 등록 빈: `SecretsKeyResolver`, `SecretsKeyClient`
- 서비스 코드에서 `SecretsKeyClient#setKey(name, spec)` 로 직접 키 주입
- 로컬/개발/테스트 환경에 적합

### 2.3 AWS 자동 모드
```properties
secrets.enabled=true
aws.secrets-manager.enabled=true
aws.secrets-manager.region=ap-northeast-2
aws.secrets-manager.secret-name=myapp/crypto-keyset
aws.secrets-manager.refresh-interval-millis=300000
aws.secrets-manager.fail-fast=true
```
- 등록 빈: `SecretsKeyResolver`, `SecretsManagerClient`, `SecretsLoader`, `SecretsKeyClient`
- 부팅 시 1회 즉시 로드 + 주기적 갱신
- JSON 예시
```json
{
  "aes.main":  { "algorithm": "AES",          "keySize": 256, "value": "BASE64_KEY_BYTES" },
  "hmac.auth": { "algorithm": "HMAC-SHA256",  "keySize": 256, "value": "BASE64_KEY_BYTES" }
}
```

---

## 3) 동작 흐름

```
Caller(서비스 코드)
 └─> SecretsKeyClient.setKey("aes.main", spec)
      └─> SecretsKeyResolver.updateKey("aes.main", spec)
           ├─ 이전 currentKey ≠ 새Key → backupKey 로 보관
           └─ currentKey 교체(핫스왑)

[AWS 모드]
 └─> SecretsLoader
      1) Secrets Manager GetSecretValue(secretName)
      2) JSON → Map<String, CryptoKeySpec> 파싱
      3) spec.decodeKey() & (keySize/8) 길이 검증
      4) Resolver.updateKey(...)
      5) SecretKeyRefreshListener.onSecretKeyRefreshed() 알림
```

---

## 4) 빠른 시작

### 4.1 CORE(수동) 모드 — 코드로 시드
```java
@Service
@RequiredArgsConstructor
public class CryptoService {
    private final SecretsKeyClient secrets;

    public void rotateAes256() {
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm("AES");
        spec.setKeySize(256);
        spec.setValue("BASE64_ENCODED_32B_KEY"); // 반드시 Base64
        secrets.setKey("aes.main", spec);        // 기존 키는 자동 백업
    }

    public byte[] current() { return secrets.getKey("aes.main"); }
    public byte[] backup()  { return secrets.getBackupKey("aes.main"); }
}
```

### 4.2 AWS 자동 모드 — 설정만으로 동작
```properties
secrets.enabled=true
aws.secrets-manager.enabled=true
aws.secrets-manager.region=ap-northeast-2
aws.secrets-manager.secret-name=myapp/crypto-keyset
# aws.secrets-manager.refresh-interval-millis=300000  # 기본 5분
# aws.secrets-manager.fail-fast=true                  # 초기 로드 실패 시 부팅 중단(운영 권장)
```
- 별도 `setKey()` 호출 불필요
- SecretsLoader 가 주기적으로 Secrets Manager에서 키 갱신

---

## 5) 애플리케이션 사용 예

```java
@Component
@RequiredArgsConstructor
public class JwtSigner {
    private final SecretsKeyClient secrets;

    public String sign(String payload) {
        byte[] key = secrets.getKey("hmac.auth");
        // HMAC-SHA256 서명 로직...
        return Base64.getEncoder().encodeToString(hmacSha256(key, payload.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean verify(String payload, String sigBase64) {
        byte[] key = secrets.getKey("hmac.auth");
        // 검증 로직...
        return constantTimeEquals(sigBase64, sign(payload));
    }
}
```

---

## 6) 테스트 가이드

### 6.1 CORE(수동) 모드
```java
@Test
void manual_seed_and_get() {
    ApplicationContextRunner ctx = new ApplicationContextRunner()
        .withPropertyValues("secrets.enabled=true")
        .withConfiguration(UserConfigurations.of(SecretsManualConfig.class));

    ctx.run(c -> {
        SecretsKeyClient client = c.getBean(SecretsKeyClient.class);
        CryptoKeySpec spec = new CryptoKeySpec();
        spec.setAlgorithm("AES"); spec.setKeySize(256); spec.setValue(base64Key(32));
        client.setKey("aes.main", spec);
        assertThat(client.getKey("aes.main")).isNotNull();
    });
}
```

### 6.2 AWS 자동 모드(스텁/통합)
- 통합: LocalStack(Secrets Manager) + `SecretsAutoConfig` 로 실제 로드 검증
- 단위: `SecretsLoader#refreshSecrets()` 호출 시 JSON 파싱/검증/등록/리스너 호출 여부 확인

---

## 7) 보안 권장사항
- **Base64 표준 인코딩** 필수(`CryptoKeySpec.value`)
- 길이 준수: AES-128=16B, AES-256/HMAC=32B (`keySize/8`)
- 키 원문/베이스64 **로그 출력 금지**
- 운영은 **IAM 최소권한**(`secretsmanager:GetSecretValue`) + `fail-fast=true` 권장
- 롤링 시 기존 키는 자동 백업 → 문제 시 **백업 승격**으로 즉시 롤백

---

## 8) 에러/예외 메시지
- `IllegalStateException`: `getKey()` 시 키 미로드(수동: `setKey()` 누락, 자동: 권한/네트워크/secretName 오류)
- `IllegalArgumentException`: 길이 불일치(`decoded.length != keySize/8`)
- 리스너 콜백 실패: 개별 로그 후 나머지 리스너는 계속 호출

---

## 9) 설정 레퍼런스

### 9.1 CORE(수동) 모드
```properties
secrets.enabled=true
```

### 9.2 AWS 자동 모드
```properties
secrets.enabled=true
aws.secrets-manager.enabled=true
aws.secrets-manager.region=ap-northeast-2
aws.secrets-manager.secret-name=myapp/crypto-keyset
aws.secrets-manager.refresh-interval-millis=300000
aws.secrets-manager.fail-fast=true
```

---

## 10) 설계 원칙
- 기본은 **OFF**
- 조건 만족 시에만 **조건부 빈 등록**
- **핫스왑 + 백업 보존**으로 무중단 키 교체
- 라이브러리 클래스에 `@Component` 금지(구성/수명주기 통제)

---

## 11) 클래스 다이어그램 (개념)

```
SecretsAutoConfig  ─┬─> SecretsManagerProperties
                    ├─> SecretsKeyResolver
                    ├─> SecretsManagerClient
                    ├─> SecretsLoader
                    └─> SecretsKeyClient

SecretsManualConfig ─┬─> SecretsKeyResolver
                     └─> SecretsKeyClient
```

---

## 12) FAQ
**Q1. 수동/자동을 동시에 켤 수 있나요?**  
A. 가능하지만 운영에선 자동 모드 권장. 자동이 로드한 키를 필요 시 코드에서 `setKey()`로 오버라이드할 수도 있습니다.

**Q2. JSON 일부만 제공(부분 시딩)해도 되나요?**  
A. 가능합니다. 미지정 키는 사용 전까지 `getKey()`에서 예외가 발생하므로, 실제 사용 키는 반드시 시딩되어야 합니다.

**Q3. 장애 시 롤백은 어떻게 하나요?**  
A. 이전 키가 자동 백업되므로 `secrets.setKey(name, specOfBackup)` 으로 **백업 승격**하면 됩니다.

---

## 13) 샘플 코드 모음

### 13.1 AES256 키 로테이션(수동)
```java
CryptoKeySpec spec = new CryptoKeySpec();
spec.setAlgorithm("AES"); spec.setKeySize(256); spec.setValue(base64Key(32));
secrets.setKey("aes.main", spec);
```

### 13.2 HMAC 키 조회
```java
byte[] key = secrets.getKey("hmac.auth");
String sig = signHmacSha256Base64(key, "payload");
```

### 13.3 리스너로 서명기 재초기화
```java
@Component
public class JwtKeyRefreshListener implements SecretKeyRefreshListener {
  public void onSecretKeyRefreshed() { jwtSigner.rebuild(); }
}
```

---

## 14) 마지막 한 줄 요약
**SecretsKeyClient 하나로 수동/자동 모두 단순 사용** — 운영은 자동 로딩, 개발은 수동 시딩.  
핫스왑·백업·리스너로 **무중단 키 교체**와 **안전한 롤백**을 지원합니다.
