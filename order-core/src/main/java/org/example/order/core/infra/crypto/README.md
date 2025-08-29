# 🔐 Crypto 키 관리/암호화 모듈

Spring Boot에서 AES/HMAC 기반 암·복호화, 서명, 해시 기능을 제공하는 경량 모듈입니다.  
**자동(Seed) 모드** 또는 **수동(Manual) 모드**로 동작하며, 동일한 인터페이스(Encryptor/Signer/Hasher)로 호출 코드를 단순화할 수 있도록 설계되었습니다.

---

## 1) 구성 개요

| 클래스/인터페이스                  | 설명 |
|-------------------------------------|------|
| `CryptoInfraConfig`                 | `crypto.enabled=true` 시 활성화(단일 구성). Encryptor/Signer/Hasher/Factory 등록, `crypto.props.seed=true`이면 설정 기반 시딩 수행 |
| `Encryptor` (AES128/256/GCM)        | 대칭키 암·복호화 구현체 |
| `Signer` (HmacSha256Signer)         | 메시지 서명/검증 구현체 |
| `Hasher`                            | 단방향 해시 구현체(Bcrypt/Argon2/SHA256) |
| `EncryptorFactory`                  | 알고리즘 타입(`CryptoAlgorithmType`) → Encryptor/Signer/Hasher 매핑 |
| `EncryptionKeyGenerator`            | URL-safe Base64 랜덤 키 생성 유틸 |
| `CryptoKeyRefreshBridge`(선택)      | Secrets 자동 로드시, 리프레시 이벤트를 받아 Encryptor/Signer에 키 주입 |

> **빈 등록 원칙**  
> 라이브러리 클래스에는 `@Component` 금지.  
> 모든 빈은 **조건부(@ConditionalOnProperty, @ConditionalOnMissingBean)** 로만 등록되어 불필요한 부작용을 방지합니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)
아무 설정도 없으면 빈이 등록되지 않으며, 다른 모듈에 영향을 주지 않습니다.

### 2.2 수동(Manual) 모드
```yaml
crypto:
  enabled: true
  props:
    seed: false
```
- 등록 빈: Encryptor, Signer, Hasher, Factory
- 각 빈의 `setKey(String base64Key)` 메서드로 직접 키 주입
- `EncryptProperties` 무시
- 로컬/개발 환경에 적합

### 2.3 자동(Seed) 모드
```yaml
crypto:
  enabled: true
  props:
    seed: true

encrypt:
  aes128:
    key: BASE64_URL_SAFE_16B
  aes256:
    key: BASE64_URL_SAFE_32B
  aesgcm:
    key: BASE64_URL_SAFE_32B
  hmac:
    key: BASE64_URL_SAFE_32B
```
- 등록 빈: Encryptor, Signer, Hasher, Factory
- `encrypt.*.key` 값이 자동 주입
- 일부 속성만 지정 가능(부분 시딩 허용)
- 운영 환경에 적합

### 2.4 Core Only → Secrets 자동 로드 모드(확장)
```yaml
crypto:
  enabled: true
  props:
    seed: false

secrets:
  enabled: true

aws:
  secrets-manager:
    enabled: true
    region: ap-northeast-2
    secret-name: myapp/crypto-keyset
    refresh-interval-millis: 300000
    fail-fast: true
```
- Secrets 모듈이 AWS Secrets Manager에서 JSON 키셋을 주기적으로 로드
- `CryptoKeyRefreshBridge`가 SecretKeyRefreshListener로서 Encryptor/Signer에 `setKey()` 호출
- 서비스 코드는 그대로(Factory 호출 유지)

---

## 3) 동작 흐름

```
Caller
 └─> EncryptorFactory.get("aes256")
      └─> Aes256Encryptor.encrypt(plain)
            ├─ key 존재 여부 확인
            ├─ AES 암호화 수행
            └─ 결과 Base64/JSON 페이로드로 반환

[Secrets 자동 모드]
 └─> SecretsLoader(GetSecretValue)
      ├─ JSON → CryptoKeySpec 파싱/검증
      ├─ Resolver 저장
      └─ SecretKeyRefreshListener 콜백 → CryptoKeyRefreshBridge → Encryptor/Signer.setKey(base64)
```

- 복호화 시 Base64 디코딩 후 동일 키로 해독
- HMAC 서명 시 서명 문자열(Base64 URL-safe) 반환
- Hasher 는 단방향 해시 문자열 반환

---

## 4) 빠른 시작

### 4.1 수동 모드 키 주입
```java
@Bean
ApplicationRunner seedKeys(Aes128Encryptor aes128) {
    return args -> {
        String key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES128); // 16B URL-safe
        aes128.setKey(key);
    };
}
```

### 4.2 자동 모드
```yaml
crypto:
  enabled: true
  props:
    seed: true

encrypt:
  aes256:
    key: BASE64_URL_SAFE_32B
  hmac:
    key: BASE64_URL_SAFE_32B
```
- 설정만으로 빈이 준비됨
- 코드에서 `setKey()` 호출 불필요

### 4.3 Core Only → Secrets 자동 로드 확장(브릿지)
```java
@Component
@RequiredArgsConstructor
public class CryptoKeyRefreshBridge implements SecretKeyRefreshListener {

    private final SecretsKeyClient secrets;
    private final EncryptorFactory factory;

    @PostConstruct
    public void init() { onSecretKeyRefreshed(); }

    @Override
    public void onSecretKeyRefreshed() {
        // 필요한 키만 골라 적용(예: aes256, hmac)
        applyEnc(CryptoAlgorithmType.AES256, "aes256");
        applySig(CryptoAlgorithmType.HMAC_SHA256, "hmac");
    }

    private void applyEnc(CryptoAlgorithmType type, String name) {
        try {
            byte[] raw = secrets.getKey(name);
            String b64 = Base64Utils.encodeUrlSafe(raw);
            factory.getEncryptor(type).setKey(b64);
        } catch (Exception ignore) { /* 미제공 시 스킵 */ }
    }

    private void applySig(CryptoAlgorithmType type, String name) {
        try {
            byte[] raw = secrets.getKey(name);
            String b64 = Base64Utils.encodeUrlSafe(raw);
            factory.getSigner(type).setKey(b64);
        } catch (Exception ignore) { /* 미제공 시 스킵 */ }
    }
}
```

---

## 5) 애플리케이션 사용 예

```java
@Component
@RequiredArgsConstructor
public class CryptoService {

    private final Aes256Encryptor aes256;
    private final HmacSha256Signer hmac;

    public String encryptData(String plain) {
        return aes256.encrypt(plain);
    }

    public String signData(String data) {
        return hmac.sign(data);
    }
}
```

(Factory 경유 예)
```java
@Component
@RequiredArgsConstructor
public class CryptoFacade {

    private final EncryptorFactory factory;

    public String encrypt(String data) {
        return factory.getEncryptor(CryptoAlgorithmType.AES256).encrypt(data);
    }

    public String sign(String payload) {
        return factory.getSigner(CryptoAlgorithmType.HMAC_SHA256).sign(payload);
    }
}
```

---

## 6) 테스트 가이드

### 6.1 수동 모드 테스트
```java
@Test
void manualModeEncryptDecrypt() {
    new ApplicationContextRunner()
        .withPropertyValues("crypto.enabled=true", "crypto.props.seed=false")
        .withConfiguration(UserConfigurations.of(CryptoInfraConfig.class))
        .run(ctx -> {
            Aes128Encryptor aes128 = ctx.getBean(Aes128Encryptor.class);
            String key = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES128);
            aes128.setKey(key);
            String enc = aes128.encrypt("hello");
            assertThat(aes128.decrypt(enc)).isEqualTo("hello");
        });
}
```

### 6.2 자동 모드 테스트
```java
@Test
void autoModeKeysInjected() {
    String key256 = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.AES256);
    String keyHmac = EncryptionKeyGenerator.generateKey(CryptoAlgorithmType.HMAC_SHA256);

    new ApplicationContextRunner()
        .withPropertyValues(
            "crypto.enabled=true",
            "crypto.props.seed=true",
            "encrypt.aes256.key=" + key256,
            "encrypt.hmac.key=" + keyHmac
        )
        .withConfiguration(UserConfigurations.of(CryptoInfraConfig.class))
        .run(ctx -> {
            Aes256Encryptor aes256 = ctx.getBean(Aes256Encryptor.class);
            assertThat(aes256.isReady()).isTrue();
        });
}
```

---

## 7) 보안 권장사항
- 키는 **URL-safe Base64** 필수
- AES128=16B, AES256/AESGCM/HMAC=32B 키 길이 준수
- 키 원문/Base64 값 로그·출력 금지
- 운영 환경은 Secrets Manager/Vault 등 외부 보안 저장소 사용 권장
- 키 로테이션 시 이전 키 자동 백업 → 필요 시 즉시 롤백 가능

---

## 8) 에러/예외 메시지
- `IllegalStateException`: 키 길이 불일치 또는 미설정
- `IllegalArgumentException`: 잘못된 키 길이/포맷(Base64 오류)
- `EncryptException/DecryptException/HashException`: 암·복호화/해시 실패

---

## 9) 설정 레퍼런스

### 9.1 수동 모드
```yaml
crypto:
  enabled: true
  props:
    seed: false
```

### 9.2 자동 모드
```yaml
crypto:
  enabled: true
  props:
    seed: true

encrypt:
  aes256:
    key: BASE64_URL_SAFE_32B
  hmac:
    key: BASE64_URL_SAFE_32B
```

### 9.3 Secrets 자동 로드
```yaml
crypto:
  enabled: true
  props:
    seed: false

secrets:
  enabled: true

aws:
  secrets-manager:
    enabled: true
    region: ap-northeast-2
    secret-name: myapp/crypto-keyset
```

---

## 10) 설계 원칙
- 기본은 OFF
- 필수 조건 만족 시에만 빈 등록
- 라이브러리 클래스에는 `@Component` 금지
- 로컬/테스트 환경에서 전역 차단 가능(설정 OFF)
- 호출부는 Factory 기반으로 통일해 모드 전환 시 코드 변경 최소화

---

## 11) 클래스 다이어그램 (개념)

```
CryptoInfraConfig ─┬─> EncryptProperties(seed=true일 때만)
                   ├─> Encryptor (AES128/256/GCM)
                   ├─> Signer (HMAC-SHA256)
                   ├─> Hasher (Bcrypt/Argon2/SHA256)
                   └─> EncryptorFactory
```

---

## 12) FAQ
**Q1. 수동/자동/Secrets 모드를 동시에 켤 수 있나요?**  
A. 가능합니다. 일부는 Seed, 일부는 Secrets, 나머지는 수동 주입처럼 부분 시딩을 허용합니다. 미시딩 알고리즘은 `isReady=false` 상태로 남습니다.

**Q2. Core Only에서 Secrets로 전환 시 서비스 코드 변경이 필요한가요?**  
A. 아닙니다. 서비스는 `EncryptorFactory`만 호출하므로 브릿지 컴포넌트 추가/설정 변경만으로 전환됩니다.

---

## 13) 샘플 코드 모음

### 13.1 AES256 암·복호화
```java
String enc = aes256.encrypt("data");
String dec = aes256.decrypt(enc);
```

### 13.2 HMAC 서명
```java
String sig = hmac.sign("message");
boolean valid = hmac.verify("message", sig);
```

---

## 14) 마지막 한 줄 요약
필요할 때만 켜지고, 켜지면 AES/HMAC을 포함한 다양한 암호화 기능을 안전하게 제공하는 모듈.  
Core Only → Seed → Secrets 자동 로드까지 **설정만으로 확장**할 수 있습니다.
