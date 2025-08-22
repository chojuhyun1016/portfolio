package org.example.order.core.infra.dynamo.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

/**
 * LocalStack + DynamoDB 통합 테스트 공통 베이스
 * - 도커로 LocalStack을 기동하고, Spring Boot에 동적으로 프로퍼티 주입
 * - "Manual 모드"로 DynamoDB 엔드포인트/리전/테이블명을 명시 주입
 * - 레디슨 오토컨피그는 테스트에서만 제외(환경 독립성)
 *
 * 주의:
 * - 프로덕션 코드는 변경하지 않는다.
 * - 엔드포인트/리전/테이블명은 여기서만 주입한다.
 */
@Testcontainers
public abstract class LocalStackDynamoSupport {

    @Container
    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer("localstack/localstack:2.3.2")
                    .withServices(DYNAMODB);

    // 통합 테스트에서 사용할 기본 테이블명
    protected static final String TABLE = "order_dynamo";

    /**
     * Spring 컨텍스트에 동적으로 프로퍼티 주입
     * - dynamodb.enabled=true               → Dynamo 설정 활성화
     * - dynamodb.region=<localstack region> → 수동 모드 리전
     * - dynamodb.endpoint=<override>        → 수동 모드 엔드포인트
     * - dynamodb.table-name=order_dynamo    → 리포지토리 등록 조건
     * - spring.autoconfigure.exclude=RedissonV2 → 테스트에서만 레디슨 자동설정 제외
     */
    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("dynamodb.enabled", () -> "true");
        registry.add("dynamodb.region", LOCALSTACK::getRegion);
        registry.add("dynamodb.endpoint", () -> LOCALSTACK.getEndpointOverride(DYNAMODB).toString());
        registry.add("dynamodb.table-name", () -> TABLE);

        // 레디슨 오토컨피그 제외(테스트 격리 목적)
        registry.add("spring.autoconfigure.exclude",
                () -> "org.redisson.spring.starter.RedissonAutoConfigurationV2");
    }

    @BeforeAll
    static void beforeAll() {
        // Testcontainers @Container 가 알아서 start() 하지만, 명시적으로 호출해도 안전
        LOCALSTACK.start();
    }

    @AfterAll
    static void afterAll() {
        LOCALSTACK.stop();
    }

    /**
     * 통합 테스트 시작 전, 테이블이 없다면 생성한다.
     * - 수동 모드로 주입된 DynamoDbClient를 사용한다.
     */
    protected static void ensureTable(DynamoDbClient dynamo) {
        try {
            dynamo.describeTable(DescribeTableRequest.builder().tableName(TABLE).build());
        } catch (ResourceNotFoundException e) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName(TABLE)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            dynamo.waiter().waitUntilTableExists(
                    DescribeTableRequest.builder().tableName(TABLE).build()
            );
        }
    }
}
