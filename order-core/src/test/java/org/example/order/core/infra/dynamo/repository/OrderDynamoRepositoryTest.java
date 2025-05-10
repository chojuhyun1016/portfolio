package org.example.order.core.infra.dynamo.repository;

import org.example.order.core.infra.dynamo.config.DynamoDbTestConfig;
import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderDynamoRepository 테스트 (로컬 DynamoDB - LocalStack 기반)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderDynamoRepositoryTest {

    private DynamoDbTestConfig config;
    private OrderDynamoRepository repository;

    @BeforeAll
    void setup() {
        config = new DynamoDbTestConfig();
        // 반드시 구현체를 생성해야 함
        repository = new OrderDynamoRepositoryImpl(config.getOrderTable());
    }

    @Test
    @Order(1)
    void saveAndFindById() {
        var order = OrderDynamoEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(1001L)
                .orderNumber("ORD-20240510")
                .orderPrice(150000L)
                .build();

        repository.save(order);

        Optional<OrderDynamoEntity> result = repository.findById(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1001L);
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD-20240510");
        assertThat(result.get().getOrderPrice()).isEqualTo(150000L);
    }

    @Test
    @Order(2)
    void findAll() {
        List<OrderDynamoEntity> all = repository.findAll();
        assertThat(all).isNotEmpty();
    }

    @Test
    @Order(3)
    void findByUserId() {
        Long userId = 1001L;
        List<OrderDynamoEntity> list = repository.findByUserId(userId);
        assertThat(list).extracting(OrderDynamoEntity::getUserId).contains(userId);
    }

    @Test
    @Order(4)
    void deleteById() {
        var order = OrderDynamoEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(2002L)
                .orderNumber("ORD-20240511")
                .orderPrice(99999L)
                .build();

        repository.save(order);

        repository.deleteById(order.getId());

        Optional<OrderDynamoEntity> deleted = repository.findById(order.getId());
        assertThat(deleted).isEmpty();
    }

    @AfterAll
    void cleanup() {
        config.deleteTableIfExists();
    }
}
