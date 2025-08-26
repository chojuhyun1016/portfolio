# ☁️ order-client.s3 모듈 — 일괄 문서

---

## 1) 모듈 개요 (현재 코드 기준)

Spring Boot 환경에서 **AWS S3**에 대해 **최소 의존**으로 업/다운로드를 수행하는 클라이언트 레이어입니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `S3Properties` | `aws.*` 네임스페이스의 설정 바인딩 | `aws.region`(필수), `aws.endpoint`(옵션, LocalStack/프록시), `aws.credential.*`(enabled 시 필수), `aws.s3.bucket`, `aws.s3.default-folder` |
| `S3Config` | `AmazonS3` 빈 구성 | **endpoint 유무에 따라** EndpointConfiguration or Region 지정, **path-style access 활성화**(LocalStack 호환), credential.enabled 시 Access/Secret 직접 주입 |
| `S3Client` | `AmazonS3` 래핑한 간단 API | `putObject(bucket, key, file)`, `getObject(bucket, key)` 제공, 예외 로깅 후 전파 |
| `S3ClientIT` | LocalStack 기반 통합 테스트 | `LocalStackContainer` + `@DynamicPropertySource`, **버킷 생성 → 업로드 → 다운로드 검증** |
| `S3ConfigBeanCreationTest` | 빈 생성 단위 테스트 | 실제 네트워크 호출 없이 **`AmazonS3` 빈 생성 여부만** 검증 |

> 현재 레이어는 **v1 SDK(AWS SDK for Java 1.x)** 기반입니다. (클래스: `com.amazonaws.services.s3.AmazonS3`)

---

## 2) 설정 (application.yml / profile)

### 2.1 최소/공통 설정 키 (코드 반영)
```yaml
aws:
  region: ap-northeast-2             # ✅ 필수
  endpoint: http://localhost:4566    # ⭕️ LocalStack/프록시 사용 시에만 (실AWS는 비움)
  credential:
    enabled: true                    # true면 아래 access/secret 필수
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    # session-token: ${AWS_SESSION_TOKEN:}   # 필요 시 확장
  s3:
    bucket: my-bucket
    default-folder: app
```

- **`aws.region`**: 필수. endpoint를 쓰더라도 `S3Config`에서 함께 사용됩니다.
- **`aws.endpoint`**: 지정 시 **EndpointConfiguration 모드**로 빌드(주로 LocalStack/프록시). 미지정 시 리전 기반.
- **`aws.credential.enabled`**: `true`면 Access/Secret을 **명시 주입**. `false`면 **기본 자격증명 체인**(EC2 Role 등) 사용.
- **`aws.s3.bucket`/`default-folder`**: 기본 버킷/폴더. `S3Properties#fullPath()`에서 조합해 사용 가능.

### 2.2 로컬(LocalStack) 예시
```yaml
# application-local.yml
aws:
  region: us-east-1
  endpoint: http://localhost:4566
  credential:
    enabled: true
    access-key: test
    secret-key: test
  s3:
    bucket: local-bucket
    default-folder: tmp
```

> `S3Config`는 **`enablePathStyleAccess()`**를 활성화하여 LocalStack와의 호환성을 확보합니다.

---

## 3) 사용법 (현재 API에 정확히 맞춤)

### 3.1 파일 업로드
```java
@Autowired S3Client s3Client; // 주입 받는 구조로 사용할 것을 권장 (아래 "빈 구성" 참고)

public void uploadExample(File file) {
    String bucket = "my-bucket";
    String key = "app/hello.txt"; // props.getS3().getDefaultFolder() 활용 가능
    s3Client.putObject(bucket, key, file);
}
```

### 3.2 파일 다운로드(읽기)
```java
S3Object obj = s3Client.getObject("my-bucket", "app/hello.txt");
try (InputStream in = obj.getObjectContent()) {
    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    // ... use body
}
```

> 현재 `S3Client`는 **파일 기반 업로드/단일 GetObject**만 제공합니다. 스트림 업로드/메타 조회/삭제/목록 등은 **확장 포인트**입니다(§7 개선안 참고).

---

## 4) 빈 구성/주입 흐름

```
S3Properties (@ConfigurationProperties "aws")
        ↓
S3Config.amazonS3Client()  ──>  AmazonS3 (SDK v1)
        ↓
S3Client(amazonS3)         ──>  서비스에서 호출 (put/get)
```

- `S3Config`가 `AmazonS3` 빈을 노출합니다.
- **현재 코드에서는 `S3Client`가 @Component/@Bean으로 등록되어 있지 않으므로**, 실제 사용 시 빈으로 등록해 주입받는 패턴을 추천합니다.

> 예: `@Configuration`에서 `@Bean S3Client s3Client(AmazonS3 s3) { return new S3Client(s3); }`

---

## 5) 통합 테스트 (LocalStack)

### 5.1 `S3ClientIT` (코드 반영 해설)
- **LocalStack 2.3.2** 이미지로 **S3 서비스** 활성화
- `@DynamicPropertySource`로 **aws.* 설정**을 Spring 컨텍스트에 주입
- 테스트 로직
    1) 버킷 존재 확인 후 없으면 생성
    2) 임시 파일 생성 → **S3Client.putObject**로 업로드
    3) **S3Client.getObject**로 내려받아 콘텐츠 검증(UTF-8 `"hello-s3"`)

#### 핵심 코드 스냅샷
```java
@Container
static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
        .withServices(LocalStackContainer.Service.S3);

@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("aws.region", LOCALSTACK::getRegion);
    r.add("aws.endpoint", () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    r.add("aws.credential.enabled", () -> "true");
    r.add("aws.credential.access-key", LOCALSTACK::getAccessKey);
    r.add("aws.credential.secret-key", LOCALSTACK::getSecretKey);
    r.add("aws.s3.bucket", () -> "it-bucket-" + UUID.randomUUID());
    r.add("aws.s3.default-folder", () -> "it-folder");
}
```

> `S3Config`는 위 속성을 받아 **LocalStack 엔드포인트 + path-style + 명시 자격증명**으로 `AmazonS3`를 생성합니다.

---

## 6) 빈 생성 단위 테스트

### 6.1 `S3ConfigBeanCreationTest` (코드 반영 해설)
- 실제 네트워크 호출 없이 **`AmazonS3` 빈 생성 여부만** 검증합니다.
- `@TestPropertySource`로 **엔드포인트/자격증명/버킷/폴더/리전**을 주입합니다.

#### 스냅샷
```java
@SpringBootTest(classes = S3Config.class)
@TestPropertySource(properties = {
        "aws.region=us-east-1",
        "aws.endpoint=http://localhost:4566",
        "aws.credential.enabled=true",
        "aws.credential.access-key=dummy",
        "aws.credential.secret-key=dummy",
        "aws.s3.bucket=test-bucket",
        "aws.s3.default-folder=tmp"
})
class S3ConfigBeanCreationTest {
    @Autowired AmazonS3 amazonS3;

    @Test
    void amazonS3BeanCreated() {
        assertNotNull(amazonS3);
    }
}
```

---

## 7) 확장/개선 제안 (현 구조 유지 전제, 선택)

> **기존 코드를 바꾸지 않고도** 안전하게 확장 가능한 지점들입니다.

- **빈 등록**: `S3Client`를 `@Bean` 또는 `@Component`로 등록해 서비스 레이어에서 **주입 사용** 권장.
  ```java
  @Configuration
  public class S3ClientConfig {
      @Bean
      public S3Client s3Client(AmazonS3 amazonS3) {
          return new S3Client(amazonS3);
      }
  }
  ```
- **예외 체계 표준화**: 현재는 로깅 후 예외 전파. 필요 시 `S3Exception`(런타임)으로 래핑하여 호출부 정책 통일.
- **API 확장**: 삭제/목록/메타조회/스트림 업로드 등 (예: `putObject(bucket, key, InputStream, ObjectMetadata)`).
- **멀티파트 업로드**: 대용량 파일 성능 향상을 위해 `TransferManager`(v1) 도입 고려.
- **테스트 안정성**: IT에서 **버킷/키 네임스페이스**를 UUID로 격리(이미 반영), 종료 훅에서 임시파일 클린업 보강.

---

## 8) 운영 팁 & 권장 설정

- **권한(IAM)**: 최소 권한(예: `s3:GetObject`, `PutObject`, `ListBucket`) 원칙.
- **엔드포인트**: 실제 AWS에서는 `aws.endpoint`를 비우고 **리전 모드**를 사용.
- **객체 키 설계**: `service/domain/yyyy/MM/dd/...` 파티셔닝으로 조회/정리 효율화.
- **로깅**: 업/다운로드 실패 시 **버킷/키/요청 ID**를 포함해 원인 추적 용이성 확보.

---

## 9) FAQ (현재 코드에 맞춘 Q/A)

- **Q. LocalStack가 아니고 실 AWS에 배포하려면?**  
  **A.** `aws.endpoint` 제거(또는 비워둠) + `aws.region` 유지. `credential.enabled=false`로 두고 EC2/ECS Role을 권장.

- **Q. 버킷이 없으면 자동 생성하나요?**  
  **A.** 아니오. 현재 테스트(`S3ClientIT`)처럼 **존재 확인 후 생성** 로직을 직접 수행해야 합니다.

- **Q. Multipart나 Presign도 지원하나요?**  
  **A.** 현재 `S3Client`는 단순 put/get만 제공합니다. 필요 시 메서드 추가로 확장 가능합니다.

---

## 10) 핵심 코드 스니펫(반영 확인)

### 10.1 `S3Config` 요지
```java
AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
        .enablePathStyleAccess(); // LocalStack 호환

if (endpoint != null && !endpoint.isBlank()) {
    builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, props.getRegion()));
} else {
    builder.withRegion(props.getRegion());
}

if (props.getCredential() != null && props.getCredential().isEnabled()) {
    var creds = new BasicAWSCredentials(props.getCredential().getAccessKey(),
                                        props.getCredential().getSecretKey());
    builder.withCredentials(new AWSStaticCredentialsProvider(creds));
}
return builder.build();
```

### 10.2 `S3Client` 요지
```java
public void putObject(String bucketName, String key, File file) {
    try {
        amazonS3.putObject(new PutObjectRequest(bucketName, key, file));
    } catch (Exception e) {
        log.error("error : upload object failed", e);
        throw e;
    }
}

public S3Object getObject(String bucketName, String key) {
    return amazonS3.getObject(new GetObjectRequest(bucketName, key));
}
```

---

## 11) 마지막 한 줄 요약
**현 구조는 “`aws.*` yml → `S3Config` → `AmazonS3` → `S3Client(put/get)`”의 단순 파이프라인**입니다.  
LocalStack/실AWS 모두 **설정만 바꿔** 동일한 사용 패턴으로 운용 가능합니다.
