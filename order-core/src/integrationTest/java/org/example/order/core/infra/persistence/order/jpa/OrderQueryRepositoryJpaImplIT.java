package org.example.order.core.infra.persistence.order.jpa;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.IntegrationBoot;
import org.example.order.core.support.AbstractIntegrationTest;
import org.example.order.core.infra.persistence.order.support.OrderInfraTestConfig;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderView;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
@Import(OrderQueryRepositoryJpaImplIT.JpaTxConfig.class)
class OrderQueryRepositoryJpaImplIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class JpaTxConfig {
        @Bean(name = "jpaTxManager")
        public PlatformTransactionManager jpaTxManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
    }

    @Autowired
    private OrderQueryRepository orderQueryRepository;

    @Autowired
    private TsidFactory tsidFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("fetchByOrderId(): 단건 프로젝션 조회")
    @Transactional(transactionManager = "jpaTxManager")
    void fetchByOrderId_single_projection() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DELETE FROM `order`");

        LocalDateTime now = LocalDateTime.now();

        OrderEntity e = OrderEntity.createEmpty();
        e.setId(tsidFactory.create().toLong());
        e.updateAll(7L, "U-7", 9999L, "O-9999", 1234L, false, 0L,
                now, 1L, "SYS", now, 1L, "SYS", now);

        jdbc.update("""
                        INSERT INTO `order`(id,user_id,user_number,order_id,order_number,order_price,delete_yn,version,
                                            published_datetime,created_user_id,created_user_type,created_datetime,
                                            modified_user_id,modified_user_type,modified_datetime)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                e.getId(), e.getUserId(), e.getUserNumber(),
                e.getOrderId(), e.getOrderNumber(), e.getOrderPrice(),
                (e.getDeleteYn() != null && e.getDeleteYn()) ? "Y" : "N",
                e.getVersion(), e.getPublishedDatetime(),
                e.getCreatedUserId(), e.getCreatedUserType(), e.getCreatedDatetime(),
                e.getModifiedUserId(), e.getModifiedUserType(), e.getModifiedDatetime()
        );

        Optional<OrderView> opt = orderQueryRepository.fetchByOrderId(9999L);

        assertThat(opt).isPresent();
        OrderView p = opt.get();
        assertThat(p.orderId()).isEqualTo(9999L);
        assertThat(p.orderNumber()).isEqualTo("O-9999");
        assertThat(p.orderPrice()).isEqualTo(1234L);
    }
}
