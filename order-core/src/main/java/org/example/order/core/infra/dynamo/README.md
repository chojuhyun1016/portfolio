# 🗄️ DynamoDB 연동 모듈

Spring Boot 환경에서 AWS DynamoDB를 간단하고 안전하게 연동하기 위한 경량 모듈입니다.  
이제는 **단일 구성(DynamoInfraConfig)** 으로 수동(Manual)과 자동(Auto) 모드를 모두 처리하며,  
서비스 코드를 바꾸지 않고도 설정만으로 모드를 전환할 수 있도록 설계되었습니다.

---

## 1) 구성 개요

| 클래스/인터페이스           | 설명 |
|-----------------------------|------|
| `DynamoInfraConfig`         | `dynamodb.enabled=true` 일 때만 빈 조립. endpoint 또는 access/secret 지정 시 **수동 모드**, 아니면 **자동 모드** |
| `DynamoDbProperties`        | `dynamodb.*` 설정 프로퍼티 매핑 |
| `OrderDynamoRepositoryImpl` | 테이블명을 설정하면 자동 등록되는 리포지토리 구현체 |
| `DynamoQuerySupport`        | DynamoDB Enhanced Client 기반 스캔/검색 유틸 모음 |

> **빈 등록 원칙**
> - 라이브러리 클래스에는 `@Component` 금지
> - 모든 빈은 **조건부(@ConditionalOnProperty, @ConditionalOnMissingBean, @ConditionalOnBean)** 로만 등록
> - 불필요한 환경에서 DynamoDB 관련 빈이 생성되지 않도록 보장

---

## 2) 동작 모드

### 2.1 OFF (기본)
아무 설정도 없으면 DynamoDB 관련 빈이 등록되지 않으며, 다른 모듈에 영향을 주지 않습니다.

### 2.2 수동(Manual) 모드
```yaml
dynamodb:
  enabled: true
  endpoint: http://localhost:4566
  region: ap-northeast-2
  access-key: local
  secret-key: local
  table-name: order_dynamo
```
- 등록 빈: `DynamoDbClient(StaticCredentials 또는 endpointOverride)`, `DynamoDbEnhancedClient`, `OrderDynamoRepositoryImpl(table-name 설정 시)`
- LocalStack/개발 환경에 적합
- endpoint 또는 access/secret 지정 시 수동 모드로 동작

### 2.3 자동(Auto) 모드
```yaml
dynamodb:
  enabled: true
  region: ap-northeast-2
  table-name: order_dynamo
```
- 등록 빈: `DynamoDbClient(DefaultCredentialsProvider)`, `DynamoDbEnhancedClient`, `OrderDynamoRepositoryImpl(table-name 설정 시)`
- AWS IAM Role/환경변수 인증 사용
- 운영 환경에 적합

---

## 3) 동작 흐름

```text
Caller
 └─> OrderDynamoRepositoryImpl
      └─> DynamoDbEnhancedClient
            └─> DynamoDbClient
                 - Manual: endpointOverride 또는 StaticCredentials
                 - Auto  : DefaultCredentialsProvider
```

---

## 4) 빠른 시작

### 4.1 수동 모드(로컬/LocalStack)
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderDynamoRepository repo;

    public void saveOrder(OrderDynamoEntity order) {
        repo.save(order);
    }
}
```

```yaml
dynamodb:
  enabled: true
  endpoint: http://localhost:4566
  region: ap-northeast-2
  access-key: local
  secret-key: local
  table-name: order_dynamo
```

### 4.2 자동 모드(IAM/운영)
```yaml
dynamodb:
  enabled: true
  region: ap-northeast-2
  table-name: order_dynamo
```
- IAM Role 또는 환경변수 인증을 자동 사용합니다.

---

## 5) 애플리케이션 사용 예

```java
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
```

---

## 6) 테스트 가이드

### 6.1 수동 모드 테스트
```java
@Test
void manualModeWorks() {
    ApplicationContextRunner ctx = new ApplicationContextRunner()
        .withPropertyValues(
            "dynamodb.enabled=true",
            "dynamodb.endpoint=http://localhost:4566",
            "dynamodb.region=ap-northeast-2",
            "dynamodb.access-key=local",
            "dynamodb.secret-key=local",
            "dynamodb.table-name=order_dynamo"
        )
        .withConfiguration(UserConfigurations.of(DynamoInfraConfig.class));

    ctx.run(context -> {
        OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);
        assertThat(repo).isNotNull();
    });
}
```

### 6.2 자동 모드 테스트
```java
@Test
void autoModeWorks() {
    ApplicationContextRunner ctx = new ApplicationContextRunner()
        .withPropertyValues(
            "dynamodb.enabled=true",
            "dynamodb.region=ap-northeast-2",
            "dynamodb.table-name=order_dynamo"
        )
        .withConfiguration(UserConfigurations.of(DynamoInfraConfig.class));

    ctx.run(context -> {
        OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);
        assertThat(repo).isNotNull();
    });
}
```

---

## 7) 보안 권장사항
- 운영 환경은 **IAM Role 기반 인증** 사용
- Access/Secret Key는 환경변수·Secrets Manager 등 외부 보관
- 최소 권한(Least Privilege) 정책 적용
- 테이블명 하드코딩 지양, 환경별 분리 권장

---

## 8) 에러/예외 메시지
- `No bean named 'orderDynamoRepository'` : `dynamodb.table-name` 미설정 (리포지토리 미등록)
- `Unable to connect to endpoint` : endpoint 설정 오류 또는 네트워크 문제
- `Unable to load AWS credentials` : Auto 모드에서 자격 증명 소스 부재

---

## 9) 설정 레퍼런스

### 9.1 수동 모드
```yaml
dynamodb:
  enabled: true
  endpoint: http://localhost:4566
  region: ap-northeast-2
  access-key: local
  secret-key: local
  table-name: order_dynamo
```

### 9.2 자동 모드
```yaml
dynamodb:
  enabled: true
  region: ap-northeast-2
  table-name: order_dynamo
```

---

## 10) 설계 원칙
- 기본은 OFF
- 수동 조건 충족 시 Manual, 아니면 Auto
- 단일 구성(`DynamoInfraConfig`)만 유지 (manual/auto 분리 제거)
- 조건부 빈 등록으로 환경별 부작용 최소화

---

## 11) 클래스 다이어그램 (개념)

```text
DynamoInfraConfig ─┬─> DynamoDbProperties
                   ├─> DynamoDbClient (Manual: StaticCredentials/endpointOverride, Auto: DefaultCredentials)
                   ├─> DynamoDbEnhancedClient
                   └─> OrderDynamoRepositoryImpl (table-name 설정 시)
```

---

## 12) FAQ

**Q1. Manual/Auto를 동시에 켤 수 있나요?**  
A. endpoint 또는 access/secret 지정 시 Manual이 우선. 그렇지 않으면 Auto.

**Q2. table-name 없으면 어떻게 되나요?**  
A. 리포지토리는 등록되지 않고 Client들만 활성화됩니다. 필요 시 직접 EnhancedClient를 활용하세요.

---

## 13) 마지막 한 줄 요약
**환경 설정만으로 Manual 또는 Auto 모드로 자동 전환되는 단일 DynamoDB 연동 모듈.**
