# ☁️ order-client.s3 모듈

---

## 1) 모듈 개요 (현행 코드 기준)

Spring Boot 환경에서 **AWS S3**를 대상으로 한 **경량 클라이언트 인프라 모듈**이다.  
AWS SDK v1(`com.amazonaws.services.s3.AmazonS3`)을 사용하며,  
**AutoConfiguration + 조건부 빈 등록** 방식으로 필요할 때만 활성화되도록 설계되어 있다.

본 모듈은 **버킷 부트스트랩이나 스케줄 작업을 포함하지 않으며**,  
오직 `AmazonS3` 와 이를 감싼 `S3Client` 빈만 제공하는 **순수 클라이언트 레이어**에 집중한다.

| 구성요소 | 역할 | 핵심 포인트 (현행 코드 반영) |
|---|---|---|
| `S3AutoConfiguration` | S3 자동 구성 진입점 | `aws.s3.enabled=true`일 때만 활성, AmazonS3 / S3Client 생성 |
| `S3Properties` | 설정 바인딩 | `aws.*` 루트 바인딩, region / endpoint / credential / s3 하위 설정 |
| `AmazonS3` | AWS SDK v1 클라이언트 | endpoint 존재 시 EndpointConfiguration + path-style, 미존재 시 region 기반 |
| `S3Client` | 서비스 래퍼 | put/get/exists/metadata API 제공, 예외 로깅 후 그대로 전파 |

> SDK: **AWS SDK for Java v1** (`AmazonS3ClientBuilder`)

---

## 2) 활성 조건 및 설계 원칙

- **핵심 스위치**
  - `aws.s3.enabled=true` 인 경우에만 AutoConfiguration이 동작한다.
- **빈 생성 정책**
  - `AmazonS3`: `@ConditionalOnMissingBean`
  - `S3Client`: `@ConditionalOnBean(AmazonS3.class)` + `@ConditionalOnMissingBean`
- **책임 분리**
  - 버킷 생성, prefix 생성, 업로드 정책 등은 **다른 모듈/서비스 책임**
  - 본 모듈은 “연결 + 단순 IO”까지만 담당

---

## 3) 설정 (application.yml)

### 3.1 기본 설정 (현행 코드 기준)

    aws:
      region: ap-northeast-2
      endpoint: http://localhost:4566
      credential:
        enabled: true
        access-key: ${AWS_ACCESS_KEY_ID}
        secret-key: ${AWS_SECRET_ACCESS_KEY}
      s3:
        enabled: true
        bucket: my-bucket
        default-folder: app
        auto-create: false
        create-prefix-placeholder: true

설명:

- `aws.s3.enabled`
  - **AutoConfiguration 활성 스위치**
  - false면 AmazonS3 / S3Client 빈 자체가 생성되지 않는다.
- `aws.region`
  - endpoint 미사용 시 필수
  - endpoint 사용 시에도 `EndpointConfiguration(endpoint, region)`에 함께 전달된다.
- `aws.endpoint`
  - LocalStack / 프록시 환경에서만 사용
  - 설정 시 `pathStyleAccessEnabled(true)` 자동 적용
- `aws.credential.enabled`
  - true면 AccessKey / SecretKey를 명시적으로 주입
  - false면 **기본 자격증명 체인(IAM Role 등)** 사용
- `aws.s3.bucket`, `aws.s3.default-folder`
  - 기본 경로 정보
  - `S3Properties#fullPath()` → `"bucket/defaultFolder"`

주의:
- AutoConfiguration 조건은 **`aws.s3.enabled`**
- Properties 내부에도 `aws.s3.enabled` 필드가 존재하므로,  
  설정 시 **두 스위치를 혼동하지 않도록 주의**한다.

---

### 3.2 로컬(LocalStack) 예시

    aws:
      region: us-east-1
      endpoint: http://localhost:4566
      credential:
        enabled: true
        access-key: test
        secret-key: test
      s3:
        enabled: true
        bucket: local-bucket
        default-folder: tmp

동작 요약:
- EndpointConfiguration 사용
- path-style access 활성화
- 명시 자격증명 사용

---

## 4) 빈 생성 흐름

    aws.s3.enabled=true
        ↓
    S3AutoConfiguration (@AutoConfiguration)
        ↓
    AmazonS3 (@ConditionalOnMissingBean)
        - endpoint 있으면 EndpointConfiguration(endpoint, region)
        - endpoint 없으면 withRegion(region)
        - credential.enabled=true면 BasicAWSCredentials 적용
        - ClientConfiguration 기본 생성
        ↓
    S3Client (@ConditionalOnBean AmazonS3)
        - AmazonS3 래퍼

---

## 5) 사용법 (현행 API 기준)

### 5.1 파일 업로드

    @Autowired
    private S3Client s3Client;

    public void upload(File file) {
        String bucket = "my-bucket";
        String key = "app/hello.txt";
        s3Client.putObject(bucket, key, file);
    }

### 5.2 메타데이터 포함 업로드

    ObjectMetadata meta = new ObjectMetadata();
    meta.setContentType("text/plain");

    s3Client.putObject("my-bucket", "app/hello.txt", file, meta);

### 5.3 파일 다운로드

    S3Object obj = s3Client.getObject("my-bucket", "app/hello.txt");
    try (InputStream in = obj.getObjectContent()) {
        byte[] data = in.readAllBytes();
    }

### 5.4 존재 여부 / 메타데이터

    boolean exists = s3Client.doesObjectExist("my-bucket", "app/hello.txt");
    ObjectMetadata meta = s3Client.getObjectMetadata("my-bucket", "app/hello.txt");

---

## 6) S3Client 정책 (중요)

- 모든 메서드는:
  - 내부에서 **에러 로그 출력**
  - 예외를 **절대 삼키지 않고 그대로 throw**
- 따라서:
  - 호출부에서 재시도/보상/알림 정책을 명확히 가져가야 한다.
- 본 모듈은:
  - “안전한 래퍼”가 아니라
  - **명시적 실패(fail-fast)** 를 지향한다.

---

## 7) 테스트 전략 (권장)

현행 코드에는 테스트가 포함되어 있지 않지만, 구조상 권장되는 테스트는 다음과 같다.

1) 조건부 빈 테스트
- `aws.s3.enabled=false`
  - AmazonS3 / S3Client 빈이 생성되지 않아야 한다.
- `aws.s3.enabled=true`
  - AmazonS3 / S3Client 빈 생성 확인

2) LocalStack 통합 테스트
- LocalStack S3 컨테이너 기동
- endpoint + path-style + 명시 자격증명 설정
- 흐름:
  - 버킷 생성
  - putObject
  - getObject
  - 콘텐츠 검증

---

## 8) 확장 포인트 (현 구조 유지 전제)

- 스트림 업로드 API 추가
  - putObject(bucket, key, InputStream, ObjectMetadata)
- 삭제 / 목록 API
  - deleteObject, listObjects
- 멀티파트 업로드
  - TransferManager(v1) 기반
- Presigned URL 발급
- ClientConfiguration 확장
  - timeout, retry, proxy 설정 노출

---

## 9) 현행 코드 요약

- AutoConfiguration 기반
- aws.s3.enabled 스위치로 완전 제어
- endpoint 존재 시 LocalStack 친화적 구성
- AWS SDK v1 유지
- 최소 기능 + 명시적 실패 정책

---

## 10) 마지막 한 줄 요약

**“`aws.s3.enabled` 하나로 S3 클라이언트를 명확히 제어하고,  
endpoint/region/credential 설정만으로 LocalStack과 실 AWS를 동일한 코드 경로로 운용하는  
경량 S3 클라이언트 모듈이다.”**
