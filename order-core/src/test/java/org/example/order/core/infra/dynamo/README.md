# 🧰 DynamoDB 모듈 테스트 가이드 (README)

Spring Boot 전체 컨텍스트 없이도 **ApplicationContextRunner**와 **Testcontainers(LocalStack)** 를 활용해,  
DynamoDB 모듈의 **ON/OFF**, **Manual/Auto 조건부 로딩**, 그리고 **실제 CRUD** 동작을 검증합니다.

---

## 📌 무엇을 테스트하나요?

아래 4가지를 **단위/경량 통합** 수준에서 확인합니다.

1) **OFF 모드**: `dynamodb.enabled=false` 시 어떤 빈도 로드되지 않는지
2) **MANUAL 모드**: `endpoint` 또는 `access-key/secret-key` 지정 시 Manual 경로가 우선 적용되는지
3) **AUTO 모드**: Manual 조건이 없을 때 Auto 경로로 클라이언트/향상클라이언트가 로딩되는지
4) **실제 CRUD (Manual + LocalStack)**: 수동 모드에서 실제 테이블을 만들고 `OrderDynamoRepository` 로 CRUD 가 동작하는지

---

## 🧩 사용 기술

- **ApplicationContextRunner**  
  조건부 자동 구성(`@ConditionalOnProperty`, `@ConditionalOnMissingBean`)을 **메인 클래스 없이** 빠르게 검증

- **Testcontainers(LocalStack)**  
  로컬 Docker 컨테이너로 **DynamoDB 호환 환경**을 띄워 **실제 CRUD** 검증

- **AssertJ / JUnit5**  
  빈 존재/부재, 모드별 로딩 결과, CRUD 결과를 선언적으로 검증

---

## 🧪 테스트 코드 전체

### 1) ON/OFF & Manual/Auto 토글 검증 — `DynamoAutoManualToggleTest`

```java
package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.dynamo.config.DynamoAutoConfig;
import org.example.order.core.infra.dynamo.config.DynamoManualConfig;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ON/OFF 및 Manual/Auto 조합에 따른 빈 로딩 결과 검증
 * - 네트워크 의존 없이 컨텍스트 레벨에서 빠르게 검사
 */
class DynamoAutoManualToggleTest {

    @Test
    void when_disabled_then_no_beans() {
        new ApplicationContextRunner()
                .withPropertyValues("dynamodb.enabled=false")
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class, DynamoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DynamoDbClient.class);
                    assertThat(ctx).doesNotHaveBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).doesNotHaveBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_manual_condition_then_manual_wins() {
        // endpoint 또는 access/secret 지정 → Manual 조건 충족
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.endpoint=http://localhost:4566",
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.table-name=order_dynamo"
                )
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class, DynamoAutoConfig.class))
                .run(ctx -> {
                    // 클라이언트/향상클라이언트 로드
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    // table-name 있으면 리포지토리 등록
                    assertThat(ctx).hasSingleBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_auto_condition_then_auto_loads_clients() {
        // Manual 조건 미충족: endpoint/access/secret 없음 → Auto
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.region=ap-northeast-2"
                        // table-name 미지정 → repo 미등록
                )
                .withConfiguration(UserConfigurations.of(DynamoAutoConfig.class, DynamoManualConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).doesNotHaveBean(OrderDynamoRepository.class);
                });
    }

    @Test
    void when_auto_with_tableName_then_repo_registered() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.table-name=order_dynamo"
                )
                .withConfiguration(UserConfigurations.of(DynamoAutoConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DynamoDbClient.class);
                    assertThat(ctx).hasSingleBean(DynamoDbEnhancedClient.class);
                    assertThat(ctx).hasSingleBean(OrderDynamoRepository.class);
                });
    }
}
```

---

### 2) Manual + LocalStack 통합 테스트 — `DynamoManualIntegrationTest`

```java
package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.dynamo.config.DynamoManualConfig;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.*;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual 모드 통합 테스트
 * - LocalStack(DynamoDB) 컨테이너를 띄워 실제 CRUD 검증
 * - ApplicationContextRunner 로 필요한 빈만 최소 기동
 * - 테이블은 테스트가 직접 생성/삭제 (모듈은 테이블 생성 책임 없음)
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DynamoManualIntegrationTest {

    // LocalStack (dynamodb 서비스만 활성화)
    @Container
    static final GenericContainer<?> localstack = new GenericContainer<>("localstack/localstack:1.4")
            .withEnv("SERVICES", "dynamodb")
            .withExposedPorts(4566);

    private ApplicationContextRunner ctx;
    private String endpoint;
    private final String tableName = "order_dynamo";

    @BeforeEach
    void initRunner() {
        endpoint = "http://" + localstack.getHost() + ":" + localstack.getMappedPort(4566);

        // Manual 모드: endpoint 또는 access/secret 중 하나라도 지정되면 Manual
        ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "dynamodb.enabled=true",
                        "dynamodb.endpoint=" + endpoint,
                        "dynamodb.region=ap-northeast-2",
                        "dynamodb.access-key=dummy",
                        "dynamodb.secret-key=dummy",
                        "dynamodb.table-name=" + tableName
                )
                .withConfiguration(UserConfigurations.of(DynamoManualConfig.class));
    }

    @Test
    @Order(1)
    void createTable_and_CRUD() {
        ctx.run(context -> {
            // 1) 클라이언트 꺼내서 테이블 생성 (없으면)
            DynamoDbClient client = context.getBean(DynamoDbClient.class);
            ensureTableExists(client, tableName);

            // 2) 리포지토리로 CRUD 검증
            OrderDynamoRepository repo = context.getBean(OrderDynamoRepository.class);

            // save
            var order = OrderDynamoEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(1001L)
                    .orderNumber("ORD-12345")
                    .orderPrice(150_000L)
                    .build();

            repo.save(order);

            // findById
            Optional<OrderDynamoEntity> loaded = repo.findById(order.getId());
            assertThat(loaded).isPresent();
            assertThat(loaded.get().getOrderNumber()).isEqualTo("ORD-12345");

            // findAll (최소 1개 이상)
            List<OrderDynamoEntity> all = repo.findAll();
            assertThat(all).isNotEmpty();

            // findByUserId
            List<OrderDynamoEntity> byUser = repo.findByUserId(1001L);
            assertThat(byUser).extracting(OrderDynamoEntity::getUserId).contains(1001L);

            // deleteById
            repo.deleteById(order.getId());
            assertThat(repo.findById(order.getId())).isEmpty();
        });
    }

    @AfterEach
    void dropTable() {
        // 컨텍스트 없이 직접 SDK로 정리 (있어도 되고 없어도 됨)
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.AP_NORTHEAST_2)
                .build()) {
            ListTablesResponse tables = client.listTables();
            if (tables.tableNames().contains(tableName)) {
                client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 존재하지 않으면 테이블 생성 + ACTIVE 대기
     */
    private static void ensureTableExists(DynamoDbClient client, String table) {
        ListTablesResponse list = client.listTables();
        if (!list.tableNames().contains(table)) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(table)
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                    )
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            waitActive(client, table);
        }
    }

    private static void waitActive(DynamoDbClient client, String table) {
        for (int i = 0; i < 30; i++) {
            DescribeTableResponse res = client.describeTable(DescribeTableRequest.builder().tableName(table).build());
            if (res.table().tableStatus() == TableStatus.ACTIVE) return;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new IllegalStateException("DynamoDB table not ACTIVE: " + table);
    }
}
```

---

## ⚙️ 속성 기반 모드 제어

- **OFF**: `dynamodb.enabled=false` → 어떤 빈도 로드되지 않음
- **MANUAL**: `dynamodb.enabled=true` 이면서 **아래 중 하나라도** 지정되면 Manual 경로
    - `dynamodb.endpoint` (예: `http://localhost:4566`)
    - 또는 `dynamodb.access-key` / `dynamodb.secret-key`
    - 선택: `dynamodb.table-name` 지정 시 `OrderDynamoRepository` 빈 등록
- **AUTO**: `dynamodb.enabled=true` 이지만 Manual 조건이 **모두 없음**
    - 보통 `dynamodb.region` 만 지정
    - 선택: `dynamodb.table-name` 지정 시 `OrderDynamoRepository` 빈 등록

예시 속성:

```properties
# OFF
dynamodb.enabled=false

# MANUAL (LocalStack)
dynamodb.enabled=true
dynamodb.endpoint=http://localhost:4566
dynamodb.region=ap-northeast-2
dynamodb.access-key=dummy
dynamodb.secret-key=dummy
dynamodb.table-name=order_dynamo

# AUTO (IAM 등 기본 자격 증명 제공 환경)
dynamodb.enabled=true
dynamodb.region=ap-northeast-2
# dynamodb.table-name=order_dynamo   # 선택: 지정 시 Repository 빈 등록
```

---

## 🧾 주의사항

- **테이블 생성 책임은 테스트/운영 스크립트에 있습니다.** 모듈은 **테이블을 만들지 않습니다.**
- Manual/AUTO **동시 조건**이 충족되면 **Manual이 우선**합니다.
- `table-name` 을 지정해야 `OrderDynamoRepository` 빈을 등록합니다(미지정 시 클라이언트들만 로딩).
- LocalStack 사용 시 Docker 환경이 필요합니다.

---

## 🚀 실행 방법

```bash
# 토글/조건부 로딩 검증
./gradlew :order-core:test --tests "org.example.order.core.infra.dynamo.DynamoAutoManualToggleTest"

# Manual + LocalStack 통합 CRUD
./gradlew :order-core:test --tests "org.example.order.core.infra.dynamo.DynamoManualIntegrationTest"
```

---

## ✅ 체크리스트

- [x] `dynamodb.enabled` OFF 시 빈 미등록
- [x] Manual 조건(엔드포인트/액세스키)이 있으면 Manual 경로 적용
- [x] Auto 조건에서 클라이언트/향상클라이언트 로딩
- [x] `table-name` 지정 시 Repository 빈 등록
- [x] LocalStack 환경에서 실제 CRUD 검증 완료
