package org.example.order.core.infra.dynamo;

import org.example.order.core.TestBootApp;
import org.example.order.core.infra.dynamo.config.DynamoManualConfig;
import org.example.order.core.infra.dynamo.support.LocalStackDynamoSupport;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LocalStack (Docker) 기반 통합 테스트
 * - 수동 모드(엔드포인트/리전/테이블명 명시)로 DynamoDB와 실제 통신
 * - 리포지토리의 public API (findById, findAll, findByUserId, deleteById) 를 실제 테이블에 대해 검증
 *
 * 주의:
 * - 프로덕션 코드는 변경하지 않는다.
 * - 기존 주석/구조를 훼손하지 않는다.
 */
@SpringBootTest(
        classes = TestBootApp.class,
        properties = {
                // Dynamo 구성 조건 충족 — 수동 모드
                "dynamodb.enabled=true",
                "dynamodb.table-name=order_dynamo"
                // endpoint, region 은 LocalStackDynamoSupport 의 @DynamicPropertySource 에서 주입
        }
)
@ActiveProfiles("test-integration")
@Import(DynamoManualConfig.class) // 테스트에서만 명시적으로 ManualConfig 보장 (프로덕션 코드 변경 없음)
class DynamoRepositoryIT extends LocalStackDynamoSupport {

    @Autowired
    DynamoDbClient dynamo;

    @Autowired
    DynamoDbEnhancedClient enhanced;

    @Autowired
    OrderDynamoRepository repository;

    private DynamoDbTable<OrderDynamoEntity> table() {
        return enhanced.table(TABLE, TableSchema.fromBean(OrderDynamoEntity.class));
    }

    private static OrderDynamoEntity e(String id, long userId) {
        var x = new OrderDynamoEntity();
        x.setId(id);
        x.setUserId(userId);
        return x;
    }

    @BeforeEach
    void setUp() {
        // 테이블 없으면 생성
        ensureTable(dynamo);

        // 테스트 데이터 초기화 (깨끗한 상태 보장)
        try {
            table().deleteItem(e("o-1", 0));
            table().deleteItem(e("o-2", 0));
            table().deleteItem(e("o-3", 0));
            table().deleteItem(e("o-4", 0));
        } catch (Exception ignore) {
            // 존재하지 않아도 무시
        }
    }

    @Test
    void end_to_end_findById() {
        // given
        table().putItem(e("o-1", 100L));

        // when
        Optional<OrderDynamoEntity> found = repository.findById("o-1");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    void end_to_end_findAll() {
        // given
        table().putItem(e("o-1", 1L));
        table().putItem(e("o-2", 2L));
        table().putItem(e("o-3", 3L));

        // when
        List<OrderDynamoEntity> all = repository.findAll();

        // then
        assertThat(all).extracting(OrderDynamoEntity::getId)
                .contains("o-1", "o-2", "o-3");
    }

    @Test
    void end_to_end_findByUserId() {
        // given
        table().putItem(e("o-1", 1L));
        table().putItem(e("o-2", 2L));
        table().putItem(e("o-3", 1L));

        // when
        List<OrderDynamoEntity> u1 = repository.findByUserId(1L);

        // then
        assertThat(u1).extracting(OrderDynamoEntity::getId)
                .containsExactlyInAnyOrder("o-1", "o-3");
    }

    @Test
    void end_to_end_deleteById() {
        // given
        table().putItem(e("o-4", 9L));

        // when
        repository.deleteById("o-4");

        // then
        Optional<OrderDynamoEntity> after = repository.findById("o-4");
        assertThat(after).isEmpty();
    }
}
