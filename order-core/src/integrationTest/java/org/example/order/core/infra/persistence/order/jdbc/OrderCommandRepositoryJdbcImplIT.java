package org.example.order.core.infra.persistence.order.jdbc;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.IntegrationBoot;
import org.example.order.core.infra.persistence.order.support.OrderInfraTestConfig;
import org.example.order.core.support.AbstractIntegrationTest;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderBatchOptions;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = {
                IntegrationBoot.JpaItSlice.class,
                OrderInfraTestConfig.class
})
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import(OrderInfraTestConfig.class)
class OrderCommandRepositoryJdbcImplIT extends AbstractIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.3.0")
            .withDatabaseName("order_it")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        r.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        r.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");

        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private OrderCommandRepository orderCommandRepository;

    @Autowired
    private TsidFactory tsidFactory;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        this.jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DELETE FROM `order`");
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("DELETE FROM `order`");
    }

    @Test
    @DisplayName("bulkInsert(): 모든 행이 삽입된다")
    void bulkInsert_shouldInsertAll() {
        LocalDateTime base = LocalDateTime.now();

        List<OrderEntity> batch = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OrderEntity e = OrderEntity.createEmpty();
            e.setId(tsidFactory.create().toLong());

            e.updateAll(
                    1L, "U-1",
                    1000L + i, "O-" + (1000 + i),
                    1000L + i, false, 0L,
                    base, 1L, "SYS", base,
                    1L, "SYS", base
            );

            batch.add(e);
        }

        OrderBatchOptions options = OrderBatchOptions.builder()
                .batchChunkSize(1000)
                .build();

        orderCommandRepository.bulkInsert(batch, options);

        Long cnt = jdbc.queryForObject("SELECT COUNT(*) FROM `order`", Long.class);
        assertThat(cnt).isEqualTo(5L);
    }

    @Test
    @DisplayName("bulkUpdate(): 조건에 매칭된 행만 업데이트된다")
    void bulkUpdate_shouldUpdateMatchedRows() {
        LocalDateTime t1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime t2 = LocalDateTime.now();

        List<OrderEntity> init = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            OrderEntity e = OrderEntity.createEmpty();
            e.setId(tsidFactory.create().toLong());

            e.updateAll(
                    9L, "U-9",
                    2000L + i, "O-200" + i,
                    3000L, false, 0L,
                    t1, 1L, "SYS", t1,
                    1L, "SYS", t1
            );

            init.add(e);
        }

        OrderBatchOptions options = OrderBatchOptions.builder()
                .batchChunkSize(1000)
                .build();

        orderCommandRepository.bulkInsert(init, options);

        List<OrderUpdate> updates = List.of(
                new OrderUpdate(
                        9L, "U-9", 2000L, "O-2000", 9999L,
                        t2, false,
                        1L, "SYS", t2,
                        2L, "SYS", t2
                ),
                new OrderUpdate(
                        9L, "U-9", 2999L, "O-2999", 8888L,
                        t2, false,
                        1L, "SYS", t2,
                        2L, "SYS", t2
                )
        );

        orderCommandRepository.bulkUpdate(updates, options);

        Long price2000 = jdbc.queryForObject("SELECT order_price FROM `order` WHERE order_id = 2000", Long.class);
        Long price2001 = jdbc.queryForObject("SELECT order_price FROM `order` WHERE order_id = 2001", Long.class);

        assertThat(price2000).isEqualTo(9999L);
        assertThat(price2001).isEqualTo(3000L);
    }
}
