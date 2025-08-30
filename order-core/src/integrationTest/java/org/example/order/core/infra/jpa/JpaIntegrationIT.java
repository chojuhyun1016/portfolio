package org.example.order.core.infra.jpa;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import org.example.order.core.IntegrationBootApp;
import org.example.order.core.infra.jpa.repository.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA만 올라오는 통합 테스트
 * - ApplicationContext = IntegrationBootApp.JpaItSlice (내부 JPA 전용 슬라이스)
 * - Redis/Redisson/Lock은 아예 스캔되지 않음
 */
@SpringBootTest(
        classes = IntegrationBootApp.JpaItSlice.class,
        properties = {
                "jpa.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:jpa_it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.redis.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "redisson.enabled=false"
        }
)
class JpaIntegrationIT {

    @Resource
    private EntityManager em;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SpringDataOrderJpaRepository orderJpa;

    private long rowId1, rowId2;
    private long orderId1, orderId2;

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();

        rowId1 = 900000000000001L;
        orderId1 = 1001L;
        rowId2 = 900000000000002L;
        orderId2 = 1002L;

        jdbcTemplate.update("DELETE FROM `order`");

        String insertSql = """
                    INSERT INTO `order` (
                        id, user_id, user_number, order_id, order_number, order_price,
                        published_datetime, delete_yn, created_user_id, created_user_type,
                        created_datetime, modified_user_id, modified_user_type, modified_datetime, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Timestamp nowTs = Timestamp.valueOf(now);

        jdbcTemplate.update(insertSql,
                rowId1, 1L, "U-1", orderId1, "O-1001", BigInteger.valueOf(10000),
                nowTs, (byte) 0, 1L, "SYS",
                nowTs, 1L, "SYS", nowTs, 0
        );

        jdbcTemplate.update(insertSql,
                rowId2, 2L, "U-2", orderId2, "O-1002", BigInteger.valueOf(20000),
                nowTs, (byte) 0, 1L, "SYS",
                nowTs, 1L, "SYS", nowTs, 0
        );
    }

    @Test
    void context_loaded_and_repos_wired() {
        assertThat(em).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
        assertThat(orderJpa).isNotNull();
    }

    @Test
    void spring_data_repo_can_read_entities() {
        assertThat(orderJpa.findById(rowId1)).isPresent();
        assertThat(orderJpa.findById(rowId2)).isPresent();
    }

    @Test
    void sql_update_changes_price_and_is_observable() {
        jdbcTemplate.update(
                "UPDATE `order` SET order_price = ? WHERE order_id = ?",
                BigInteger.valueOf(11234), orderId1
        );

        assertThat(orderJpa.findById(rowId1)).isPresent();

        var price = jdbcTemplate.queryForObject(
                "SELECT order_price FROM `order` WHERE order_id = ?",
                BigDecimal.class, orderId1
        );

        assertThat(price).isEqualByComparingTo(new BigDecimal("11234"));
    }
}
