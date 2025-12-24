# 🔐 infra:common.secrets — AWS Secrets Manager 기반 키 로딩/선택 모듈 (스케줄러 옵트인)

Spring Boot 환경에서 **AES/HMAC 등 암·복호화용 Secret Key**를  
AWS Secrets Manager로부터 안전하게 **로딩 · 선택 · 갱신**하기 위한 경량 인프라 모듈입니다.

현행 구현은 **설정 기반(@Bean) + 조건부 활성화**를 원칙으로 하며,  
전역 `@Scheduled` / `@EnableScheduling` 에 의존하지 않고 **주입된 `TaskScheduler`로만** 동작합니다.

> 📌 본 문서는 `org.example.order.core.infra.common.secrets` **현재 코드 기준(현행화)** 으로 작성되었습니다.

-------------------------------------------------------------------------------

## 1) 구성 개요 (현재 코드 기준)

### 핵심 클래스/역할

| 구성요소 | 역할 | 핵심 포인트 (현행 코드 반영) |
|---|---|---|
| `SecretsInfraConfig` | 모듈 진입점 | `aws.secrets-manager.enabled=true` 일 때만 전체 활성 |
| `SecretsManagerProperties` | 설정 바인딩 | `aws.*` + `aws.secrets-manager.*` 단일 클래스 바인딩 (`@ConfigurationProperties("aws")`) |
| `SecretsKeyResolver` | 키 스냅샷/선택 관리 | alias별 다중 키 스냅샷(store) + 현재 선택 포인터(pointer) 관리, 동시성 안전 |
| `SecretsKeyClient` | 서비스 진입점 | Resolver에 대한 **얇은 래퍼** (`setSnapshot / getKey / applySelection`) |
| `SecretsLoader` | AWS 로딩 파이프라인 | 초기 로드 + (옵션) 주기 갱신 + LocalStack 부트스트랩 |
| `SecretKeyRefreshListener` | 후처리 훅 | 키 갱신 후 선택 정책/캐시 재구성용 콜백 |
| `CryptoKeySpec` | Secrets JSON DTO | Base64 디코딩 책임만 가짐 (`decodeKey()`) |
| `CryptoKeySpecEntry` | 내부 정규화 모델 | alias/kid/version/algorithm/keyBytes |

### 설계 원칙

- 라이브러리 클래스에 `@Component` 사용 금지
- **설정 기반(@Bean) + 조건부 등록**만 사용
- 전역 스케줄링 미사용 (`@Scheduled` 없음)
- **키 선택 정책은 Loader가 아니라 Resolver/Initializer 책임**
- **키 바이트는 절대 로그에 남기지 않음**(메타만 로그)

-------------------------------------------------------------------------------

## 2) 활성화 조건 & 프로퍼티

### 필수 게이트

- `aws.secrets-manager.enabled=true`
  - 이 값이 `false` 이면 **모든 Secrets 관련 빈 미등록**

### 스케줄러 동작 조건 (옵트인)

- `aws.secrets-manager.scheduler-enabled=true`
- AND `TaskScheduler` 빈 존재
  - 없으면 모듈 내부에서 `ThreadPoolTaskScheduler(pool=1)` 를 **자동 생성**(조건부)

### 초기 로드/주기 갱신 정책 (현행 코드 기준)

- **초기 로드(1회)**: 항상 수행
  - `ApplicationReadyEvent`에서 `refreshOnce()` 실행
- **주기 갱신(fixedDelay)**: 스케줄러 옵트인일 때만 수행
  - `scheduler-enabled=true` AND `TaskScheduler` 사용 가능 → `scheduleWithFixedDelay`

> ⚠️ 문서 현행화 포인트  
> 기존 README에 “scheduler-enabled=false면 초기 1회 로드도 안 함(완전 유휴)”라고 되어 있었다면,  
> **현행 코드에서는 초기 1회 로드를 수행**합니다. (주기 갱신만 OFF)

-------------------------------------------------------------------------------

## 3) 설정 프로퍼티 (YAML) — 현재 코드 기준

### 운영 예시: 초기 1회 + 주기 갱신

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: true
        refresh-interval-millis: 300000
        fail-fast: true

### 운영 예시: 초기 1회만(주기 갱신 OFF)

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: false
        fail-fast: true

### LocalStack 예시: endpoint + 정적 크리덴셜

    aws:
      endpoint: http://localhost:4566
      credential:
        enabled: true
        access-key: test
        secret-key: test
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: true
        refresh-interval-millis: 300000
        fail-fast: true

### 프로퍼티 상세 (현행)

- `aws.secrets-manager.enabled` (boolean)
- `aws.secrets-manager.region` (string, default: `ap-northeast-2`)
- `aws.secrets-manager.secret-name` (string, 운영 필수)
- `aws.secrets-manager.refresh-interval-millis` (long, default: 300000, min: 1000)
- `aws.secrets-manager.fail-fast` (boolean, default: true)
- `aws.secrets-manager.scheduler-enabled` (boolean, default: false)
- `aws.endpoint` (string, LocalStack 등)
- `aws.credential.enabled` (boolean, default: false)
- `aws.credential.access-key` / `aws.credential.secret-key` (string)

-------------------------------------------------------------------------------

## 4) Secrets Manager JSON 포맷 (현행)

> alias → value가 **object 또는 array** 모두 지원됩니다.

### 4.1 단일 키(object)

    {
      "order.aesgcm": {
        "kid": "key-2025-01",
        "version": 1,
        "algorithm": "AES-256-GCM",
        "key": "BASE64_KEY_BYTES"
      }
    }

### 4.2 다중 키(array, 롤링/백업)

    {
      "order.aesgcm": [
        {
          "kid": "key-2024-12",
          "version": 1,
          "algorithm": "AES-256-GCM",
          "key": "BASE64_KEY_BYTES_OLD"
        },
        {
          "kid": "key-2025-01",
          "version": 2,
          "algorithm": "AES-256-GCM",
          "key": "BASE64_KEY_BYTES_NEW"
        }
      ]
    }

### 규칙/주의

- `key` : Base64 또는 URL-safe Base64 → `CryptoKeySpec.decodeKey()`에서 디코딩  
  (현행은 `Base64Utils.decodeFlexible(key)` 사용)
- `kid`, `version` : 선택
- `algorithm` : 필수(문자열 메타). 알고리즘 유효성 검증/매칭은 상위 Crypto 모듈 책임

-------------------------------------------------------------------------------

## 5) 동작 흐름 (현재 코드 기준)

### 5.1 애플리케이션 기동 시(초기 1회)

    ApplicationReadyEvent
     └─ SecretsLoader.onApplicationReady()
         ├─ refreshOnce()
         │   ├─ GetSecretValue(secretName)
         │   ├─ JSON → Map<String, Object>
         │   ├─ object|array → CryptoKeySpec 변환
         │   ├─ CryptoKeySpec.decodeKey() → bytes
         │   ├─ CryptoKeySpecEntry(alias,kid,version,algorithm,keyBytes) 정규화
         │   ├─ SecretsKeyResolver.setSnapshot(alias, entries)
         │   └─ SecretKeyRefreshListener.onSecretKeyRefreshed() 통지
         └─ (옵트인) scheduleWithFixedDelay(safeRefresh, interval)

### 5.2 주기 갱신(fixedDelay) — 옵트인

    aws.secrets-manager.scheduler-enabled=true
    AND TaskScheduler 존재(없으면 내부에서 생성)
      └─ TaskScheduler.scheduleWithFixedDelay(this::safeRefresh, Duration.ofMillis(interval))

- interval은 `max(1000, refresh-interval-millis)`로 하한 적용

### 5.3 LocalStack 특례

- endpoint host가 다음 중 하나면 LocalStack으로 간주:
  - `localhost`, `127.0.0.1`, `localstack`, `*.localstack.cloud`
- Secret이 없으면:
  - `"{}"` 로 `createSecret` 또는 `putSecretValue`로 부트스트랩 후 재조회

-------------------------------------------------------------------------------

## 6) 키 선택/조회 정책 (Resolver) — 현재 코드 기준

### 6.1 선택 규칙 (`applySelection(alias, version, kid, allowLatest)`)

선택 우선순위:

1) `kid` 일치
2) `version` 일치
3) `allowLatest=true` 인 경우 최신 version(max) 선택

선택 성공 시:

- alias별 pointer(`AtomicReference<CryptoKeySpecEntry>`)가 고정됨
- 이후 `getKey(alias)`는 pointer의 keyBytes 반환

### 6.2 조회 API (SecretsKeyClient)

- 현재 선택된 키

  byte[] key = secrets.getKey("order.aesgcm");

- 과거 버전/특정 kid 조회 (롱테일 복호화 등)

  byte[] byKid = secrets.getKey("order.aesgcm", null, "key-2024-12");
  byte[] byVer = secrets.getKey("order.aesgcm", 1, null);

> 주의
> - 선택이 한 번도 적용되지 않으면 `getKey(alias)`는 `IllegalStateException`
> - `getKey(alias, version, kid)`는 조건 미지정/미존재 시 `null` 가능

-------------------------------------------------------------------------------

## 7) 서비스 코드 사용 예

### 7.1 AES-GCM 사용 예(개념)

    @Component
    @RequiredArgsConstructor
    public class OrderCryptoService {

        private final org.example.order.core.infra.common.secrets.client.SecretsKeyClient secrets;

        public byte[] encrypt(byte[] plain) {
            byte[] key = secrets.getKey("order.aesgcm"); // 선택된 키
            // AES-GCM encrypt...
            return encryptAesGcm(key, plain);
        }
    }

### 7.2 키 갱신 후 후처리(선택)

    @Component
    public class CryptoKeyRefreshListener
            implements org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener {

        @Override
        public void onSecretKeyRefreshed() {
            // 예: 핀 정책 재적용(운영 기본: allowLatest=false 권장)
            // secrets.applySelection("order.aesgcm", 2, null, false);
        }
    }

-------------------------------------------------------------------------------

## 8) 실패/예외 처리 정책 (현재 코드 기준)

### 8.1 초기 로드 실패

- `fail-fast=true` AND `!LocalStack`  
  → `IllegalStateException` 던져 **기동 중단**(운영 권장)
- LocalStack으로 간주되면  
  → 경고 로그 후 기동 지속

### 8.2 secret-name 미설정/공백

- fail-fast=true AND !LocalStack이면 중단
- 그렇지 않으면 경고 후 skip

### 8.3 선택 없이 현재 키 조회

- `SecretsKeyResolver.getCurrentKey(alias)`  
  → `IllegalStateException("No selected key for alias=...")`

### 8.4 리스너 예외

- 리스너 예외는 개별 로깅 후 다음 리스너 계속 호출(전파 안 함)

-------------------------------------------------------------------------------

## 9) 보안 체크리스트 (현행 코드 반영)

- 키 값(바이트) 로깅 금지
  - `setSnapshot`에서 `kid/version/algorithm` 메타만 JSON 로깅
- 종료/회수 시 `wipeAll()` 호출로 메모리 키 zero-fill 권장
- 운영은 IAM 최소권한:
  - `secretsmanager:GetSecretValue`
  - (LocalStack 부트스트랩 사용 시) `CreateSecret`, `PutSecretValue` 필요
- 운영 기본 정책:
  - `allowLatest=false`(자동 최신 전환 금지) + 핀(kid/version) 중심 운영 권장

-------------------------------------------------------------------------------

## 10) 클래스 다이어그램(개념)

    SecretsInfraConfig
    ├─ Core (aws.secrets-manager.enabled=true)
    │  ├─ SecretsKeyResolver
    │  └─ SecretsKeyClient
    └─ AwsLoader (SecretsManagerClient 클래스패스 감지)
       ├─ SecretsManagerClient
       ├─ (옵션) TaskScheduler
       └─ SecretsLoader
            └─ SecretKeyRefreshListener* (0..n)

-------------------------------------------------------------------------------

## 11) FAQ (현행 코드 기준)

Q. 전역 `spring.task.scheduling.enabled=false` 인데, `scheduler-enabled=true` 이면 주기 동작하나요?  
A. 동작합니다. 본 모듈은 `@Scheduled` 를 사용하지 않고, 주입된 `TaskScheduler`로만 등록합니다.

Q. `scheduler-enabled=false` 면 초기 1회 로드도 안 하나요?  
A. 아닙니다. **현행 코드는 초기 1회 로드를 수행**하고, 주기 갱신만 하지 않습니다.

Q. 키 선택은 어디서 하나요?  
A. Loader는 로딩만 담당합니다.  
선택/핀 정책은 `SecretKeyRefreshListener` 또는 별도 초기화 로직에서 `applySelection(...)`으로 수행하는 것을 권장합니다.

-------------------------------------------------------------------------------

## 12) 한 줄 요약

**`aws.secrets-manager.enabled`로 인프라를 켜고,  
`scheduler-enabled`로 주기 갱신을 옵트인하며,  
키 선택은 Resolver에서 명시적으로 제어한다.**

→ 운영/로컬 모두 안전한 **Secrets Manager 기반 키 관리 표준 모듈**입니다.
