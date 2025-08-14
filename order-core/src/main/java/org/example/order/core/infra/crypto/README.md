# 🔐 Crypto 키 관리/암호화 모듈

Spring Boot에서 AES/HMAC 기반 암·복호화, 서명, 해시 기능을 제공하는 경량 모듈입니다.  
**자동(Seed) 모드** 또는 **수동(Manual) 모드**로 동작하며, 동일한 인터페이스(Encryptor/Signer/Hasher)로 호출 코드를 단순화할 수 있도록 설계되었습니다.

---

## 1) 구성 개요

| 클래스/인터페이스                  | 설명 |
|-------------------------------------|------|
| `CryptoManualConfig`                | `crypto.enabled=true` 시 수동 모드 활성화, 빈 등록 |
| `CryptoAutoConfig`                  | `crypto.enabled=true` & `crypto.props.seed=true` 시 자동 모드 활성화 |
| `Encryptor` (AES128/256/GCM)         | 대칭키 암·복호화 구현체 |
| `Signer` (HmacSha256Signer)         | 메시지 서명/검증 구현체 |
| `Hasher`                            | 단방향 해시 구현체 |
| `EncryptorFactory`                  | 알고리즘명 → Encryptor 매핑 |
| `SignerFactory`                     | 알고리즘명 → Signer 매핑 |
| `HasherFactory`                     | 알고리즘명 → Hasher 매핑 |

> **빈 등록 원칙**  
> 라이브러리 클래스에는 `@Component` 금지.  
> 모든 빈은 **조건부(@ConditionalOnProperty, @ConditionalOnMissingBean)** 로만 등록되어 불필요한 부작용을 방지합니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)
아무 설정도 없으면 빈이 등록되지 않으며, 다른 모듈에 영향을 주지 않습니다.

### 2.2 수동(Manual) 모드
```properties
crypto.enabled=true
```
- 등록 빈: Encryptor, Signer, Hasher, Factory
- 각 빈의 `setKey(String base64Key)` 메서드로 직접 키 주입
- `EncryptProperties` 무시
- 로컬/개발 환경에 적합

### 2.3 자동(Seed) 모드
```properties
crypto.enabled=true
crypto.props.seed=true
encrypt.aes128.key=BASE64_16B_KEY
encrypt.aes256.key=BASE64_32B_KEY
encrypt.aesgcm.key=BASE64_32B_KEY
encrypt.hmac.key=BASE64_32B_KEY
```
- 등록 빈: Encryptor, Signer, Hasher, Factory
- `EncryptProperties` 값이 자동 주입
- 일부 속성만 지정 가능(부분 시딩 허용)
- 운영 환경에 적합

---

## 3) 동작 흐름

```
Caller
 └─> EncryptorFactory.get("aes256")
      └─> Aes256Encryptor.encrypt(plain)
            ├─ key 존재 여부 확인
            ├─ AES 암호화 수행
            └─ 결과 Base64 인코딩 후 반환
```

- 복호화 시 Base64 디코딩 후 동일 키로 해독
- HMAC 서명 시 서명 문자열(Base64) 반환
- Hasher 는 단방향 해시 문자열 반환

---

## 4) 빠른 시작

### 4.1 수동 모드 키 주입
```java
@Bean
ApplicationRunner seedKeys(Aes128Encryptor aes128) {
    return args -> {
        String key = base64Key(16); // 16바이트 AES128 키
        aes128.setKey(key);
    };
}
```

### 4.2 자동 모드
```properties
crypto.enabled=true
crypto.props.seed=true
encrypt.aes256.key=BASE64_32B_KEY
encrypt.hmac.key=BASE64_32B_KEY
```
- 설정만으로 빈이 준비됨
- 코드에서 `setKey()` 호출 불필요

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

---

## 6) 테스트 가이드

### 6.1 수동 모드 테스트
```java
@Test
void manualModeEncryptDecrypt() {
    ApplicationContextRunner ctx = new ApplicationContextRunner()
        .withPropertyValues("crypto.enabled=true")
        .withConfiguration(UserConfigurations.of(CryptoManualConfig.class));

    ctx.run(context -> {
        Aes128Encryptor aes128 = context.getBean(Aes128Encryptor.class);
        String key = base64Key(16);
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
    String key256 = base64Key(32);
    String keyHmac = base64Key(32);

    ApplicationContextRunner ctx = new ApplicationContextRunner()
        .withPropertyValues(
            "crypto.enabled=true",
            "crypto.props.seed=true",
            "encrypt.aes256.key=" + key256,
            "encrypt.hmac.key=" + keyHmac
        )
        .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class));

    ctx.run(context -> {
        Aes256Encryptor aes256 = context.getBean(Aes256Encryptor.class);
        assertThat(aes256.isReady()).isTrue();
    });
}
```

---

## 7) 보안 권장사항
- 키는 **Base64 표준 인코딩** 필수
- AES128=16B, AES256/AESGCM/HMAC=32B 키 길이 준수
- 키 원문/Base64 값 로그·출력 금지
- 운영 환경은 Secrets Manager/Vault 등 외부 보안 저장소 사용 권장

---

## 8) 에러/예외 메시지
- `IllegalStateException`: 키 길이 불일치 또는 미설정
- `CryptoOperationException`: 암·복호화/서명 실패
- `Base64DecodingException`: 잘못된 Base64 문자열

---

## 9) 설정 레퍼런스

### 9.1 수동 모드
```properties
crypto.enabled=true
```

### 9.2 자동 모드
```properties
crypto.enabled=true
crypto.props.seed=true
encrypt.aes256.key=BASE64_32B_KEY
encrypt.hmac.key=BASE64_32B_KEY
```

---

## 10) 설계 원칙
- 기본은 OFF
- 필수 조건 만족 시에만 빈 등록
- 라이브러리 클래스에는 `@Component` 금지
- 로컬/테스트 환경에서 전역 차단 가능(설정 OFF)

---

## 11) 클래스 다이어그램 (개념)

```
CryptoAutoConfig ─┬─> EncryptProperties
                  ├─> EncryptorFactory
                  ├─> SignerFactory
                  └─> HasherFactory

CryptoManualConfig ─┬─> EncryptorFactory
                    ├─> SignerFactory
                    └─> HasherFactory
```

---

## 12) FAQ
**Q1. 수동/자동 모드를 동시에 켤 수 있나요?**  
A. 설정상 둘 다 true 여도 자동 모드가 우선 주입, 필요 시 수동으로도 setKey 가능. 운영은 자동 모드 권장.

**Q2. 부분 시딩이 가능한가요?**  
A. 가능. 지정되지 않은 알고리즘은 ready=false 상태로 남음.

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
자동/수동 모드를 자유롭게 전환할 수 있습니다.
