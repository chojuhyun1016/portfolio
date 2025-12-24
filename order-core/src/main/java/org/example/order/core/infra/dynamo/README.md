# 🗄️ DynamoDB 연동 모듈

Spring Boot 환경에서 AWS DynamoDB를 간단하고 안전하게 연동하기 위한 경량 모듈입니다.  
이제는 **단일 구성(DynamoInfraConfig)** 으로 수동(Manual)과 자동(Auto) 모드를 모두 처리하며,  
서비스 코드를 바꾸지 않고도 설정만으로 모드를 전환할 수 있도록 설계되었습니다.

---

## 1) 구성 개요

| 클래스/인터페이스                   | 설명 |
|------------------------------------|------|
| `DynamoInfraConfig`                | `dynamodb.enabled=true` 일 때만 빈 조립. endpoint 또는 access/secret 지정 시 **수동 모드**, 아니면 **자동 모드** |
| `DynamoDbProperties`               | `dynamodb.*` 설정 프로퍼티 매핑 |
| `OrderDynamoRepositoryImpl`        | `table-name` 설정 시 자동 등록되는 리포지토리 구현체 |
| `DynamoMigrationAutoConfiguration` | `local` 프로파일 + `auto-create=true` 조건에서 마이그레이션 자동 실행 |
| `DynamoMigrationInitializer`       | 테이블/GSI/LSI 생성, 스키마 드리프트 감지·조정, 시드 적용 |
| `DynamoMigrationLoader`            | classpath 기반 Vn 마이그레이션/시드 로더 |
| `DynamoQuerySupport`               | DynamoDB Enhanced Client 기반 스캔/검색 유틸 |

> **빈 등록 원칙**
> - 라이브러리 클래스에는 `@Component` 금지
> - 모든 빈은 **조건부(@ConditionalOnProperty, @ConditionalOnMissingBean, @ConditionalOnBean)** 로만 등록
> - `dynamodb.enabled=false` 인 환경에서는 DynamoDB 관련 빈이 **단 하나도 생성되지 않음**

---

## 2) 동작 모드

### 2.1 OFF (기본)

아무 설정도 없으면 DynamoDB 관련 빈이 등록되지 않으며,  
다른 모듈(JPA, Redis, Kafka 등)에 영향을 주지 않습니다.

---

### 2.2 수동(Manual) 모드

아래 조건 중 하나라도 만족하면 Manual 모드로 동작합니다.

- `endpoint` 지정
- `access-key` + `secret-key` 지정

#### 설정 예시 (LocalStack / 개발)

~~~yaml
dynamodb:
  enabled: true
  endpoint: http://localhost:4566
  region: ap-northeast-2
  access-key: local
  secret-key: local
  table-name: order_dynamo
~~~

#### 동작 특징

- `DynamoDbClient`
    - endpointOverride + StaticCredentialsProvider
- region 미지정 시 `us-east-1` 기본값(LocalStack 호환)
- `DynamoDbEnhancedClient` 자동 생성
- `table-name` 존재 시 `OrderDynamoRepositoryImpl` 자동 등록

---

### 2.3 자동(Auto) 모드

endpoint, access/secret 이 모두 없는 경우 Auto 모드로 동작합니다.

#### 설정 예시 (운영 / IAM)

~~~yaml
dynamodb:
  enabled: true
  region: ap-northeast-2
  table-name: order_dynamo
~~~

#### 동작 특징

- `DefaultCredentialsProvider` 사용
- IAM Role / EC2 / ECS / EKS 메타데이터 자동 탐지
- 운영 환경 권장 방식

---

## 3) 동작 흐름

~~~text
Caller
 └─> OrderDynamoRepositoryImpl
      └─> DynamoDbEnhancedClient
           └─> DynamoDbClient
                - Manual: endpointOverride / StaticCredentials
                - Auto  : DefaultCredentialsProvider
~~~

---

## 4) 빠른 시작

### 4.1 수동 모드 (로컬 / LocalStack)

~~~java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDynamoRepository repo;

    public void saveOrder(OrderDynamoEntity order) {
        repo.save(order);
    }
}
~~~

~~~yaml
dynamodb:
  enabled: true
  endpoint: http://localhost:4566
  region: ap-northeast-2
  access-key: local
  secret-key: local
  table-name: order_dynamo
~~~

---

### 4.2 자동 모드 (IAM / 운영)

~~~yaml
dynamodb:
  enabled: true
  region: ap-northeast-2
  table-name: order_dynamo
~~~

- IAM Role 또는 환경변수 인증 자동 사용
- 서비스 코드 변경 불필요

---

## 5) 애플리케이션 사용 예

~~~java
@Component
@RequiredArgsConstructor
public class OrderAppService {

    private final OrderDynamoRepository orderRepo;

    public Optional<OrderDynamoEntity> findOrder(String id) {
        return orderRepo.findById(id);
    }

    public List<OrderDynamoEntity> allOrders() {
        return orderRepo.findAll();
    }
}
~~~

---

## 6) 로컬 전용: DynamoDB 마이그레이션 & 시드

### 6.1 활성 조건 (모두 만족)

- `spring.profiles.active=local`
- `dynamodb.enabled=true`
- `dynamodb.auto-create=true`
- `DynamoDbClient` 빈 존재

---

### 6.2 리소스 구조

~~~text
resources/
└─ dynamodb/
   ├─ migration/
   │  ├─ V1__order.json
   │  └─ V2__order_gsi.json
   └─ seed/
      └─ V1__order_seed.json
~~~

규칙
- 최신 Vn 버전만 적용
- 동일 버전 파일은 병합 처리

---

### 6.3 스키마 관리 원칙

- DynamoDB는 스키마리스 → **키 구조만 관리**
- 비교 대상
    - 테이블 HASH / RANGE 키
    - GSI / LSI 정의
    - 키 타입(S / N / B)
- 모든 키는 `attributes` 에 타입 선언 필수
- 누락 시 애플리케이션 기동 실패 (fail-fast)

---

## 7) 스키마 리컨실 (Schema Reconcile)

### 기본 정책
- 기본값: DRY-RUN
- 안전 변경만 자동 적용

### 감지 항목
- TABLE_KEY_CHANGED
- LSI_CHANGED
- GSI_KEY_CHANGED
- GSI_PROJECTION_CHANGED
- KEY_TYPE_MISMATCH

### 설정 예시

~~~yaml
dynamodb:
  schema-reconcile:
    enabled: true
    dry-run: true
    allow-destructive: false
    delete-extra-gsi: false
    copy-data: false
    max-item-count: 10000
~~~

---

## 8) 테스트 가이드

### 8.1 수동 모드 테스트

~~~java
@Test
void manualModeWorks() {
    new ApplicationContextRunner()
        .withPropertyValues(
            "dynamodb.enabled=true",
            "dynamodb.endpoint=http://localhost:4566",
            "dynamodb.region=ap-northeast-2",
            "dynamodb.access-key=local",
            "dynamodb.secret-key=local",
            "dynamodb.table-name=order_dynamo"
        )
        .withConfiguration(UserConfigurations.of(DynamoInfraConfig.class))
        .run(context -> {
            OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);
            assertThat(repo).isNotNull();
        });
}
~~~

---

### 8.2 자동 모드 테스트

~~~java
@Test
void autoModeWorks() {
    new ApplicationContextRunner()
        .withPropertyValues(
            "dynamodb.enabled=true",
            "dynamodb.region=ap-northeast-2",
            "dynamodb.table-name=order_dynamo"
        )
        .withConfiguration(UserConfigurations.of(DynamoInfraConfig.class))
        .run(context -> {
            OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);
            assertThat(repo).isNotNull();
        });
}
~~~

---

## 9) 보안 권장사항

- 운영 환경은 **IAM Role 기반 인증** 사용
- Access / Secret Key 소스코드 저장 금지
- Secrets Manager / Parameter Store 사용 권장
- 최소 권한(Least Privilege) 정책 적용

---

## 10) 설계 원칙

- 기본은 OFF
- 수동 조건 충족 시 Manual, 아니면 Auto
- 단일 구성(`DynamoInfraConfig`)만 유지
- 조건부 빈 등록으로 환경별 부작용 최소화

---

## 11) 클래스 다이어그램 (개념)

~~~text
DynamoInfraConfig
 ├─> DynamoDbProperties
 ├─> DynamoDbClient
 │     ├─ Manual: StaticCredentials / endpointOverride
 │     └─ Auto  : DefaultCredentialsProvider
 ├─> DynamoDbEnhancedClient
 └─> OrderDynamoRepositoryImpl (table-name 설정 시)
~~~

---

## 12) FAQ

**Q1. Manual/Auto를 동시에 켤 수 있나요?**  
A. endpoint 또는 access/secret 지정 시 Manual이 우선 적용됩니다.

**Q2. table-name 없으면 어떻게 되나요?**  
A. 리포지토리는 등록되지 않고 Client/EnhancedClient만 활성화됩니다.

---

## 13) 마지막 한 줄 요약

**환경 설정만으로 Manual 또는 Auto 모드로 자동 전환되며,  
로컬에서는 마이그레이션·시드·스키마 관리까지 책임지는 단일 DynamoDB 인프라 모듈**
