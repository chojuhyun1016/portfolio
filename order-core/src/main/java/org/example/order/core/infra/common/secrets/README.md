# 🔐 infra:secrets — Secrets Manager / 수동 키 로딩 모듈

Spring Boot에서 AES/HMAC 등 **암·복호화용 SecretKey**를 안전하게 주입/관리하기 위한 경량 모듈입니다.  
**CORE 수동 모드** 또는 **AWS 자동 모드**로 동작하며, 서비스 코드는 **`SecretsKeyClient`** 하나로 `setKey` / `getKey` / `getBackupKey` 를 단순 호출하면 됩니다.

---

## 🔄 변경 사항 요약 (현행 구조)

- **단일 진입점 구성으로 통합:** `SecretsInfraConfig` 1개만 사용 (`@Import(SecretsInfraConfig.class)` 한 줄로 모듈 조립)
- **조건부 빈 등록 게이트:**
  - `secrets.enabled: true` → Core 빈(`SecretsKeyResolver`, `SecretsKeyClient`)만 등록
  - 추가로 `aws.secrets-manager.enabled: true` + **AWS SDK 클래스패스 존재** → AWS 로더(`SecretsLoader`, `SecretsManagerClient`)까지 등록
- **삭제됨:** `SecretsAutoConfig`, `SecretsManualConfig` (기능은 `SecretsInfraConfig`로 흡수)
- **스케줄링 범위 최소화:** AWS 로더가 활성화될 때에만 스케줄링 활성
- **중복 초기화 방지:** 애플리케이션 **준비 완료(ApplicationReadyEvent)** 한 번 실행 → 이후 **fixedDelay(이전 실행 종료 시점 기준 지연)** 주기로만 동작

---

## 1) 구성 개요

| 클래스/인터페이스             | 설명 |
|-------------------------------|------|
| `SecretsInfraConfig`          | 진입점 구성. `secrets.enabled=true` 시 활성화. Core 빈 등록. AWS 조건 충족 시 로더 포함 |
| `SecretsKeyClient`            | 서비스 코드용 얇은 래퍼: `setKey` / `getKey` / `getBackupKey` 제공 |
| `SecretsKeyResolver`          | 현재/백업 키 보관(핫스왑/롤백), 동시성 안전 |
| `SecretsLoader`               | AWS Secrets Manager에서 JSON 비밀 로드 → Resolver 반영 → 리스너 알림 |
| `SecretKeyRefreshListener`    | 로드/교체 후 콜백 인터페이스 |
| `CryptoKeySpec`               | `{ algorithm, keySize, value(Base64) }` 키 스펙 모델 |
| `SecretsManagerProperties`    | `region`, `secret-name`, `refresh-interval-millis`, `fail-fast` 등 프로퍼티 바인딩 |

> 원칙: 라이브러리 클래스에는 `@Component`를 사용하지 않고, **설정 기반(@Bean) + 조건부**로만 등록합니다.

---

## 2) 동작 모드 & 프로퍼티

### 2.1 OFF (기본)
설정이 없으면 **아무 빈도 등록되지 않음** (다른 모듈 영향 없음)

### 2.2 CORE(수동) 모드
YAML:
secrets:
enabled: true

- 등록 빈: `SecretsKeyResolver`, `SecretsKeyClient`
- 서비스 코드에서 `SecretsKeyClient#setKey(name, spec)` 로 **직접 키 주입**
- 로컬/개발/테스트 환경에 적합

### 2.3 AWS 자동 모드
YAML:
secrets:
enabled: true

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        refresh-interval-millis: 300000   # fixedDelay 주기(밀리초) — 이전 실행 "종료" 기준
        fail-fast: true                   # 초기 로드 실패 시 부팅 중단(운영 권장)

- 등록 빈: `SecretsKeyResolver`, `SecretsManagerClient`, `SecretsLoader`, `SecretsKeyClient`
- **ApplicationReadyEvent 시 1회 강제 로드 → 이후 fixedDelay 주기 갱신**

Secrets JSON 예시(Secrets Manager 저장 값):
JSON:
{
"aes.main":  { "algorithm": "AES",          "keySize": 256, "value": "BASE64_KEY_BYTES" },
"hmac.auth": { "algorithm": "HMAC-SHA256",  "keySize": 256, "value": "BASE64_KEY_BYTES" }
}

---

## 3) 동작 흐름 (핵심 로직)

CORE(수동):
(흐름도)
Caller(서비스 코드)
└─> SecretsKeyClient.setKey("aes.main", spec)
└─> SecretsKeyResolver.updateKey("aes.main", spec)
├─ 이전 currentKey ≠ 새Key → backupKey 로 보관
└─ currentKey 교체(핫스왑)

AWS 자동:
(흐름도)
ApplicationReadyEvent
└─> SecretsLoader.refreshSecrets() 최초 실행
└─> @Scheduled(fixedDelay=refresh-interval-millis) 주기 실행
1) Secrets Manager GetSecretValue(secretName)
2) JSON → Map<String, CryptoKeySpec> 파싱
3) spec.decodeKey() & (keySize/8) 길이 검증
4) Resolver.updateKey(...)
5) SecretKeyRefreshListener.onSecretKeyRefreshed() 알림

---

## 4) 애플리케이션 조립 (가장 중요한 사용법)

### 4.1 모듈 조립 (Kafka/S3/Web과 동일한 패턴)
Java:
// 애플리케이션 진입점 (또는 Infra 조립용 @Configuration)
@Import(SecretsInfraConfig.class)
@SpringBootApplication
public class App {
public static void main(String[] args) { SpringApplication.run(App.class, args); }
}

### 4.2 CORE(수동) 모드 — 코드로 시드
YAML:
secrets:
enabled: true

Java:
@Service
@RequiredArgsConstructor
public class CryptoService {
private final SecretsKeyClient secrets;

        public void rotateAes256() {
            CryptoKeySpec spec = new CryptoKeySpec();
            spec.setAlgorithm("AES");
            spec.setKeySize(256);                    // 256비트 → 32바이트
            spec.setValue("BASE64_ENCODED_32B_KEY"); // 반드시 Base64
            secrets.setKey("aes.main", spec);        // 기존 키는 자동 백업
        }

        public byte[] current() { return secrets.getKey("aes.main"); }
        public byte[] backup()  { return secrets.getBackupKey("aes.main"); }
    }

### 4.3 AWS 자동 모드 — 설정만으로 동작
YAML:
secrets:
enabled: true

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        # refresh-interval-millis: 300000  # 기본 5분 (fixedDelay)
        # fail-fast: true                  # 초기 로드 실패 시 부팅 중단(운영 권장)

- 별도 `setKey()` 호출 불필요
- `SecretsLoader`가 **준비 완료 시 최초 실행 → 이후 주기적 실행**

---

## 5) 서비스 코드 사용 예 (가장 자주 쓰는 패턴)

### 5.1 HMAC 서명/검증
Java:
@Component
@RequiredArgsConstructor
public class JwtSigner {
private final SecretsKeyClient secrets;

        public String sign(String payload) {
            byte[] key = secrets.getKey("hmac.auth");
            // HMAC-SHA256 서명 로직...
            return base64(hmacSha256(key, payload.getBytes(StandardCharsets.UTF_8)));
        }

        public boolean verify(String payload, String sigBase64) {
            byte[] key = secrets.getKey("hmac.auth");
            // 검증 로직...
            return constantTimeEquals(sigBase64, sign(payload));
        }
    }

### 5.2 리스너로 컴포넌트 재초기화
Java:
@Component
@RequiredArgsConstructor
public class JwtKeyRefreshListener implements SecretKeyRefreshListener {
private final JwtSigner signer;

        @Override
        public void onSecretKeyRefreshed() {
            // 예: 서명기 내부 캐시 재빌드 등
            // signer.rebuild();
        }
    }

---

## 6) 에러/예외와 대처 (운영에서 꼭 유의)

- `IllegalStateException`: `getKey()` 시 **키 미로드**
  - CORE: `setKey()` 누락 → 부팅 로직에서 필수 키를 선 주입
  - AWS: 권한/네트워크/`secret-name` 오류 → 프로퍼티/권한 점검, 운영은 `fail-fast: true` 권장
- `IllegalArgumentException`: **키 길이 불일치**
  - `decoded.length != keySize/8` → 예: AES-256은 32바이트 Base64 필요
- 리스너 콜백 예외: 개별 로깅 후 나머지 리스너는 계속 호출 (전체 실패로 전파하지 않음)

---

## 7) 보안 체크리스트 (Best Practice)

- **Base64 표준 인코딩** 필수 (`CryptoKeySpec.value`)
- **키 길이 준수:** AES-128=16B, AES-256/HMAC=32B
- **키/시크릿 값 로깅 금지** (알고리즘/비트수 같은 메타만 로깅)
- 운영은 **IAM 최소권한**(`secretsmanager:GetSecretValue`) + `fail-fast: true`
- **핫스왑+백업 보존**: 새 키 투입 시 이전 키 자동 백업 → 문제 시 **백업 승격**으로 즉시 롤백

---

## 8) 설정 레퍼런스 (요약, YML 기준)

CORE(수동):
YAML:
secrets:
enabled: true

AWS 자동:
YAML:
secrets:
enabled: true

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        refresh-interval-millis: 300000
        fail-fast: true

---

## 9) 클래스 다이어그램 (개념)

(다이어그램)
SecretsInfraConfig
├─> SecretsKeyResolver
├─> SecretsKeyClient
└─ AwsLoaderConfig (조건 충족 시)
├─> SecretsManagerProperties
├─> SecretsManagerClient
└─> SecretsLoader

---

## 10) FAQ (사용법 위주)

Q1. 수동/자동을 동시에 켤 수 있나요?  
A. 가능합니다. 자동이 로드한 키를 필요 시 코드에서 `setKey()`로 오버라이드할 수 있습니다.

Q2. JSON 일부만 제공(부분 시딩)해도 되나요?  
A. 가능합니다. 미지정 키는 사용 전까지 `getKey()`에서 예외가 발생하므로, 실제 사용하는 키는 반드시 시딩되어야 합니다.

Q3. 장애 시 롤백은 어떻게 하나요?  
A. 이전 키가 자동 백업되므로 `secrets.setKey(name, specOfBackup)` 으로 **백업 승격**하면 됩니다.

---

## 11) 마지막 한 줄 요약

**`@Import(SecretsInfraConfig.class)` + YML 두 줄**로 조립하고,  
**`SecretsKeyClient` 하나로 수동/자동 모두 단순 사용.**  
핫스왑·백업·리스너로 **무중단 키 교체**와 **안전한 롤백**을 지원합니다.
