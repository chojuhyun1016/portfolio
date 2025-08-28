# ☁️ order-client.s3 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring Boot 환경에서 **AWS S3**에 대해 **최소 의존**으로 업/다운로드를 수행하는 클라이언트 레이어입니다.  
최신 구조는 **설정 기반(@Bean) + `@Import` 조립**과 **조건부 빈 등록**으로, 필요할 때만 켜지도록 설계되었습니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `S3ModuleConfig` | 모듈 대표 Import | **내부 S3ClientConfig만 로드**. 실제 빈 생성 여부는 프로퍼티로 제어 |
| `config.s3client.S3ClientConfig` | `AmazonS3`/`S3Client` 구성 | `aws.s3.enabled=true`일 때만 활성. **endpoint 유무에 따라** EndpointConfiguration 또는 Region 지정, **path-style access 활성화**(LocalStack 호환), `credential.enabled` 시 Access/Secret 직접 주입 |
| `property.S3Properties` | `aws.*` 네임스페이스 설정 바인딩 | `aws.region`(필수), `aws.endpoint`(옵션, LocalStack/프록시), `aws.credential.*`(enabled 시 필수), `aws.s3.bucket`, `aws.s3.default-folder` |
| `service.S3Client` | `AmazonS3` 래퍼 | `putObject(bucket, key, file)`, `getObject(bucket, key)` 제공(예외 로깅 후 전파) |
| **테스트** | IT/단위 검증 | LocalStack 기반 통합 테스트(**버킷 생성 → 업로드 → 다운로드 검증**), enabled/disabled 조건부 빈 생성 테스트 |

> SDK는 **AWS SDK for Java v1**(`com.amazonaws.services.s3.AmazonS3`)을 사용합니다.

---

## 2) 설정 (application.yml / profile)

### 2.1 스위치 & 공통 키 (코드 반영)
```yaml
aws:
  region: ap-northeast-2             # ✅ 필수 (endpoint 미사용 시 반드시 필요)
  endpoint: http://localhost:4566    # ⭕️ LocalStack/프록시 사용 시에만(실AWS는 비움)
  credential:
    enabled: true                    # true면 아래 access/secret 필수
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
  s3:
    enabled: true                    # ✅ 이 스위치가 true일 때만 AmazonS3/S3Client 빈 생성
    bucket: my-bucket
    default-folder: app
```

- **`aws.s3.enabled`**: 핵심 스위치. `true`여야 `AmazonS3`/`S3Client`가 등록됩니다.
- **`aws.region`**: 필수. endpoint를 쓰더라도 `S3ClientConfig`에서 함께 사용됩니다.
- **`aws.endpoint`**: 지정 시 **EndpointConfiguration 모드**로 빌드(주로 LocalStack/프록시). 미지정 시 리전 기반.
- **`aws.credential.enabled`**: `true`면 Access/Secret을 **명시 주입**. `false`면 **기본 자격증명 체인**(EC2/ECS Role 등) 사용.
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
    enabled: true
    bucket: local-bucket
    default-folder: tmp
```

> `S3ClientConfig`는 **`enablePathStyleAccess()`**를 활성화하여 LocalStack와의 호환성을 확보합니다.

---

## 3) 사용법 (현재 API에 정확히 맞춤)

### 3.1 파일 업로드
```java
@Autowired S3Client s3Client; // @Bean 주입 구조 사용

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
S3ClientConfig (@ConditionalOnProperty "aws.s3.enabled=true")
   ├─ @Bean AmazonS3         ──>  SDK v1 클라이언트 (endpoint/region/credential 반영, path-style ON)
   └─ @Bean S3Client         ──>  서비스에서 호출 (put/get)
        ↓
S3ModuleConfig               ──>  외부에서 이 "대표"만 @Import
```

- **대표 구성**: 외부 모듈/앱에서는 `@Import(S3ModuleConfig.class)` 만 추가하면 됩니다.  
  실제 빈 생성은 `aws.s3.enabled`가 **true일 때만** 발생합니다.

---

## 5) 통합 테스트 (LocalStack)

### 5.1 `S3ClientIT` (코드 반영 해설)
- **LocalStack 2.3.2** 이미지로 **S3 서비스** 활성화
- 컨테이너가 뜨기 전에 호출되는 문제를 피하려고 **static 블록에서 컨테이너를 먼저 start**하고, 값을 캐시한 뒤 `@DynamicPropertySource`에서 **캐시값만** 제공합니다.
- 테스트 흐름
  1) 버킷 존재 확인 후 없으면 생성
  2) 임시 파일 생성 → **S3Client.putObject**로 업로드
  3) **S3Client.getObject**로 내려받아 콘텐츠 검증(UTF-8 `"hello-s3"`)

#### 핵심 코드 스냅샷
```java
static {
    LOCALSTACK.start();
    REGION = LOCALSTACK.getRegion();
    ENDPOINT = LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString();
    ACCESS_KEY = LOCALSTACK.getAccessKey();
    SECRET_KEY = LOCALSTACK.getSecretKey();
    BUCKET = "it-bucket-" + UUID.randomUUID();
    DEFAULT_FOLDER = "it-folder";
}

@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("aws.region", () -> REGION);
    r.add("aws.endpoint", () -> ENDPOINT);
    r.add("aws.credential.enabled", () -> "true");
    r.add("aws.credential.access-key", () -> ACCESS_KEY);
    r.add("aws.credential.secret-key", () -> SECRET_KEY);
    r.add("aws.s3.enabled", () -> "true");
    r.add("aws.s3.bucket", () -> BUCKET);
    r.add("aws.s3.default-folder", () -> DEFAULT_FOLDER);
}
```

> 위 속성으로 `S3ClientConfig`가 **LocalStack 엔드포인트 + path-style + 명시 자격증명**으로 `AmazonS3`를 생성합니다.

---

## 6) 빈 생성 단위 테스트

### 6.1 `S3ClientConfigBeanCreationTest` (업데이트)
- 실제 네트워크 호출 없이 **`AmazonS3` 빈 생성 여부만** 검증합니다.
- `@TestPropertySource`로 **endpoint/credential/region/bucket/default-folder + `aws.s3.enabled=true`**를 주입합니다.

#### 스냅샷
```java
@SpringBootTest(classes = S3ClientConfig.class)
@TestPropertySource(properties = {
        "aws.region=us-east-1",
        "aws.endpoint=http://localhost:4566",
        "aws.credential.enabled=true",
        "aws.credential.access-key=dummy",
        "aws.credential.secret-key=dummy",
        "aws.s3.enabled=true",
        "aws.s3.bucket=test-bucket",
        "aws.s3.default-folder=tmp"
})
class S3ClientConfigBeanCreationTest {
    @Autowired AmazonS3 amazonS3;
    @Test void amazonS3BeanCreated() { assertNotNull(amazonS3); }
}
```

### 6.2 Disabled 스위치 테스트
```java
@SpringBootTest(classes = S3ClientConfig.class)
@TestPropertySource(properties = {
        "aws.s3.enabled=false",
        "aws.region=us-east-1"  // 있어도 빈이 생성되지 않아야 함
})
class S3ClientConfigDisabledTest {
    @Autowired ApplicationContext ctx;
    @Test void amazonS3BeanAbsent() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(AmazonS3.class));
    }
}
```

---

## 7) 확장/개선 제안 (현 구조 유지 전제, 선택)

- **예외 표준화**: 현재는 로깅 후 예외 전파. 필요 시 `S3Exception`(런타임)으로 래핑하여 호출부 정책 통일.
- **API 확장**: 삭제/목록/메타조회/스트림 업로드 등 (예: `putObject(bucket, key, InputStream, ObjectMetadata)`).
- **멀티파트 업로드**: 대용량 파일 성능 향상을 위해 `TransferManager`(v1) 도입 고려.
- **Presigned URL**: 다운로드 권한 위임 시 유용(만료/서명 파라미터 정책 포함).
- **프로퍼티 세분화**: 연결/소켓 타임아웃, 리트라이 정책 등을 `aws.s3.client.*` 하위 키로 노출.

---

## 8) 운영 팁 & 권장 설정

- **권한(IAM)**: 최소 권한(예: `s3:GetObject`, `PutObject`, `ListBucket`) 원칙.
- **엔드포인트**: 실제 AWS에서는 `aws.endpoint`를 비우고 **Region 모드** 사용.
- **객체 키 설계**: `service/domain/yyyy/MM/dd/...` 파티셔닝으로 조회/정리 효율화.
- **로깅**: 업/다운로드 실패 시 **버킷/키/요청 ID**를 포함해 원인 추적 용이성 확보.

---

## 9) 핵심 코드 스니펫(반영 확인)

### 9.1 `config.s3client.S3ClientConfig` 요지
```java
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3ClientConfig {
  private final S3Properties props;

  @Bean
  public AmazonS3 amazonS3Client() {
    validateRequiredWhenEnabled();

    AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
        .enablePathStyleAccess(); // LocalStack 호환

    if (isNotBlank(props.getEndpoint())) {
      builder.withEndpointConfiguration(
          new AwsClientBuilder.EndpointConfiguration(props.getEndpoint(),
                                                     coalesce(props.getRegion(), "us-east-1")));
    } else {
      builder.withRegion(props.getRegion());
    }

    if (props.getCredential() != null && props.getCredential().isEnabled()) {
      String ak = props.getCredential().getAccessKey();
      String sk = props.getCredential().getSecretKey();
      if (!isNotBlank(ak) || !isNotBlank(sk)) {
        throw new IllegalStateException("aws.credential.enabled=true requires both access-key and secret-key.");
      }
      builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ak, sk)));
    }

    return builder.build();
  }

  @Bean
  public S3Client s3Client(AmazonS3 amazonS3) {
    return new S3Client(amazonS3);
  }

  // helpers: validateRequiredWhenEnabled(), isNotBlank(), coalesce() ...
}
```

### 9.2 `property.S3Properties` 요지
```java
@Validated
@ConfigurationProperties("aws")
public class S3Properties {
  private Credential credential;
  @NotBlank private String region;
  private S3 s3;
  private String endpoint;

  public static class Credential {
    private boolean enabled = true;
    @NotBlank private String accessKey;
    @NotBlank private String secretKey;
  }
  public static class S3 {
    @NotBlank private String bucket;
    @NotBlank private String defaultFolder;
    // ⚠️ 사용 스위치는 여기 말고 'aws.s3.enabled' (Boolean) 로 yml에 존재
  }
}
```

### 9.3 `service.S3Client` 요지
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

### 9.4 대표 모듈 Import (`S3ModuleConfig`)
```java
@Configuration
@Import({org.example.order.client.s3.config.s3client.S3ClientConfig.class})
public class S3ModuleConfig {}
```

---

## 10) 마지막 한 줄 요약
**“`aws.s3.enabled` 스위치 하나로 S3 클라이언트를 명확히 제어하고, endpoint/region/credential을 설정만 바꿔 LocalStack/실AWS를 동일한 코드 경로로 운용.”**  
필요 시 `S3Client.put/get`을 시작점으로 기능을 점진 확장하세요.
