# 🔐 infra:secrets — AWS Secrets Manager 기반 키 로딩 모듈 (스케줄러 옵트인)

Spring Boot에서 AES/HMAC 등 암·복호화용 SecretKey를 안전하게 로딩·갱신하기 위한 경량 모듈입니다.  
운영 기본은 AWS Secrets Manager에서 키셋을 읽어와 애플리케이션에 주입합니다.  
스케줄러는 명시적으로 켤 때만 동작하며, 전역 @Scheduled 인프라를 사용하지 않습니다.

-------------------------------------------------------------------------------

## 1) 구성 개요

표

- SecretsInfraConfig: 진입점 구성. aws.secrets-manager.enabled=true 일 때만 전체 활성
- SecretsManagerProperties: region, secret-name, refresh-interval-millis, fail-fast, scheduler-enabled 등 바인딩
- SecretsKeyResolver: 현재/백업 키 보관(핫스왑·롤백), 동시성 안전
- SecretsKeyClient: 서비스 코드 진입점. setKey, getKey, getBackupKey 제공
- SecretsLoader: AWS Secrets Manager에서 JSON 시크릿 로드 → Resolver 반영 → 리스너 알림. 스케줄러 있을 때만 초기 1회 + 주기 실행
- SecretKeyRefreshListener: 키 갱신 후 콜백 인터페이스

원칙

- 라이브러리 클래스에는 @Component를 사용하지 않고, 설정 기반(@Bean) + 조건부로만 등록
- 전역 @EnableScheduling 미사용. 필요 시 TaskScheduler 빈 주입으로만 동작

-------------------------------------------------------------------------------

## 2) 동작 조건과 프로퍼티

필수 게이트

- aws.secrets-manager.enabled=true 이어야 빈들이 생성됨

스케줄 동작

- aws.secrets-manager.scheduler-enabled=true 이고 TaskScheduler 빈이 존재할 때만
  - 애플리케이션 준비 완료 시점(ApplicationReadyEvent)에 초기 1회 로드
  - 이후 fixedDelay 로 주기 갱신
- scheduler-enabled=false 이면 초기 1회 로드조차 수행하지 않음(완전 유휴)

프로퍼티 요약(YAML 키)

- aws.secrets-manager.enabled: true/false
- aws.secrets-manager.region: 예) ap-northeast-2
- aws.secrets-manager.secret-name: 예) myapp/crypto-keyset
- aws.secrets-manager.refresh-interval-millis: 기본 300000(5분). 스케줄러 ON일 때만 의미
- aws.secrets-manager.fail-fast: 초기 로드 실패 시 부팅 중단(운영 권장 true)
- aws.secrets-manager.scheduler-enabled: 스케줄러 옵트인. true 면 초기 1회 + 주기 갱신 수행

참고: spring.task.scheduling.enabled=false 로 전역 스케줄링을 꺼도 본 모듈에는 영향 없음(내부는 주입된 TaskScheduler 로만 동작)

-------------------------------------------------------------------------------

## 3) 시크릿 JSON 포맷(Secrets Manager 저장값)

예시(JSON)

    {
      "aes.main":  { "algorithm": "AES",         "keySize": 256, "value": "BASE64_KEY_BYTES" },
      "hmac.auth": { "algorithm": "HMAC-SHA256", "keySize": 256, "value": "BASE64_KEY_BYTES" }
    }

검증 규칙

- value 는 Base64 인코딩 바이트
- keySize 비트수에 맞춰 디코딩 바이트 길이 일치해야 함(AES-256 → 32B, AES-128 → 16B, HMAC-SHA256 → 32B 등)

-------------------------------------------------------------------------------

## 4) 동작 흐름

AWS 자동(스케줄러 ON)

    ApplicationReadyEvent
    └─ SecretsLoader.refreshOnce() (초기 1회 로드)
        ├─ GetSecretValue(secretName)
        ├─ JSON → Map<String, CryptoKeySpec> 파싱
        ├─ spec.decodeKey() & (keySize/8) 길이 검증
        ├─ SecretsKeyResolver.updateKey(...)
        └─ SecretKeyRefreshListener.onSecretKeyRefreshed() 알림
    이후
    └─ TaskScheduler.scheduleWithFixedDelay(refreshOnce, refresh-interval-millis)

AWS 자동(스케줄러 OFF)

    ApplicationReadyEvent
    └─ 아무 작업 안 함(초기 1회 로드도 미수행, 완전 유휴)
    필요 시, 서비스 코드에서 SecretsKeyClient.setKey(...) 로 수동 시딩 가능

-------------------------------------------------------------------------------

## 5) 애플리케이션 조립(가장 중요한 사용법)

프로젝트에 의존성 추가 후, 구성 클래스를 임포트합니다.

Java

    @Import(org.example.order.core.infra.common.secrets.config.SecretsInfraConfig.class)
    @SpringBootApplication
    public class App {
      public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(App.class, args);
      }
    }

YAML(운영 예시: 초기 1회 + 주기 갱신)

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: true
        refresh-interval-millis: 300000
        fail-fast: true

YAML(초기 1회 로드도 없이 완전 유휴. 키는 코드에서 수동 시딩 시 사용 가능)

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: false
        fail-fast: true

-------------------------------------------------------------------------------

## 6) 서비스 코드 사용 예

HMAC 서명/검증

    @org.springframework.stereotype.Component
    @lombok.RequiredArgsConstructor
    public class JwtSigner {
      private final org.example.order.core.infra.common.secrets.client.SecretsKeyClient secrets;

      public String sign(String payload) {
        byte[] key = secrets.getKey("hmac.auth");
        // HMAC-SHA256 서명 로직...
        return base64(hmacSha256(key, payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      }

      public boolean verify(String payload, String sigBase64) {
        byte[] key = secrets.getKey("hmac.auth");
        // 상수 시간 비교 등 검증 로직...
        return constantTimeEquals(sigBase64, sign(payload));
      }
    }

키 갱신 리스너(선택)

    @org.springframework.stereotype.Component
    public class JwtKeyRefreshListener implements org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener {
      @Override
      public void onSecretKeyRefreshed() {
        // 예: 서명기 내부 캐시 재빌드 등
        // signer.rebuild();
      }
    }

수동 시딩(스케줄러 OFF/유휴 모드에서 필요 시)

    @org.springframework.stereotype.Service
    @lombok.RequiredArgsConstructor
    public class CryptoSeed {
      private final org.example.order.core.infra.common.secrets.client.SecretsKeyClient secrets;

      public void seedAes256() {
        var spec = new org.example.order.core.infra.common.secrets.model.CryptoKeySpec();
        spec.setAlgorithm("AES");
        spec.setKeySize(256);
        spec.setValue("BASE64_ENCODED_32B_KEY");
        secrets.setKey("aes.main", spec); // 기존 키는 자동 백업
      }
    }

-------------------------------------------------------------------------------

## 7) 에러/예외와 대처

- IllegalStateException: getKey 호출 시 키 미로드
  - 스케줄러 OFF(유휴) 환경에서 초기 시딩 누락 → 코드에서 setKey 로 선 주입 필요
  - 스케줄러 ON 환경에서 권한/네트워크/secret-name 오류로 초기 로드 실패 → 프로퍼티·권한 점검. 운영은 fail-fast: true 권장
- IllegalArgumentException: 키 길이 불일치
  - 디코딩 길이 != keySize/8 (AES-256은 32바이트, AES-128은 16바이트 등)
- 리스너 예외: 개별 로깅 후 나머지 리스너 호출 계속(전파 안 함)

-------------------------------------------------------------------------------

## 8) 보안 체크리스트

- value 는 표준 Base64 사용
- 키 길이 준수: AES-128=16B, AES-256/HMAC-SHA256=32B
- 키 값 로깅 금지(알고리즘/비트수 같은 메타만 로그)
- 운영은 IAM 최소권한(secretsmanager:GetSecretValue) + fail-fast: true
- 핫스왑+백업 보존: 새 키 투입 시 이전 키 자동 백업 → 문제 시 백업 승격으로 즉시 롤백

-------------------------------------------------------------------------------

## 9) 설정 레퍼런스(YAML)

스케줄러 ON(운영 일반)

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: true
        refresh-interval-millis: 300000
        fail-fast: true

스케줄러 OFF(유휴; 필요 시 수동 시딩)

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/crypto-keyset
        scheduler-enabled: false
        fail-fast: true

참고: 전역 스케줄러 비활성화

    spring:
      task:
        scheduling:
          enabled: false

(모듈은 TaskScheduler 빈 주입으로만 동작하므로 위 설정과 무관하게 제어 가능)

-------------------------------------------------------------------------------

## 10) 클래스 다이어그램(개념)

텍스트 다이어그램

    SecretsInfraConfig
    ├─ Core (aws.secrets-manager.enabled=true)
    │  ├─ SecretsManagerProperties (ConfigurationProperties 바인딩은 설정 클래스의 @Bean 한 곳에서만 수행)
    │  ├─ SecretsKeyResolver
    │  └─ SecretsKeyClient
    └─ AwsLoader (AWS SDK 클래스패스 감지)
       ├─ SecretsManagerClient
       ├─ (옵션) TaskScheduler ← scheduler-enabled=true && MissingBean 시 1스레드 생성
       └─ SecretsLoader (초기 1회 + fixedDelay 주기 갱신; 스케줄러 없으면 완전 유휴)

-------------------------------------------------------------------------------

## 11) FAQ

질문: 전역 spring.task.scheduling.enabled=false 인데, scheduler-enabled=true 이면 주기 동작하나요?  
답변: 동작합니다. 본 모듈은 @Scheduled 를 쓰지 않고, 주입된 TaskScheduler 로만 스케줄을 등록합니다.

질문: 스케줄러를 끄면 초기 1회 로드만 하고 끝나나요?  
답변: 요구사항에 따라, scheduler-enabled=false 이면 초기 1회 로드도 수행하지 않습니다(완전 유휴). 필요 시 서비스 코드에서 SecretsKeyClient.setKey 로 직접 시딩하세요.

질문: 자동 로드와 수동 시딩을 함께 써도 되나요?  
답변: 가능합니다. 자동 로드된 키를 필요에 따라 setKey 로 덮어쓸 수 있으며, 이전 키는 자동 백업됩니다.

-------------------------------------------------------------------------------

## 12) 한 줄 요약

aws.secrets-manager.enabled 로 온·오프하고, scheduler-enabled 로 초기 1회+주기 갱신까지 옵트인.  
전역 스케줄러 오염 없이 TaskScheduler 주입 기반으로 안전하게 운영됩니다.
