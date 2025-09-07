package org.example.order.core.infra.persistence.order.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.example.order.core.IntegrationBoot;
import org.example.order.core.infra.persistence.order.support.OrderInfraTestConfig;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.tsid.TsidFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 통합 테스트:
 * - 이 테스트 클래스 내부 TestConfiguration의 JpaTransactionManager만 사용 (다른 테스트와 분리)
 * - OrderEntity는 수동 PK(TSID) 방식이므로 persist 전 ID를 반드시 세팅해야 함
 */
@SpringBootTest(classes = { IntegrationBoot.JpaItSlice.class, OrderInfraTestConfig.class },
        properties = {
                "jpa.enabled=true",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"
        })
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import(OrderRepositoryJpaImplIT.JpaTxConfig.class)
class OrderRepositoryJpaImplIT {

    @TestConfiguration
    static class JpaTxConfig {
        @Bean(name = "jpaTxManager")
        public PlatformTransactionManager jpaTxManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
    }

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TsidFactory tsidFactory;

    private OrderEntity newOrder(Long id, Long orderId) {
        OrderEntity e = new OrderEntity();
        // ✅ 수동 PK: null이면 TSID 생성
        e.setId(id != null ? id : tsidFactory.create().toLong());
        e.setUserId(100L);
        e.setUserNumber("U-100");
        e.setOrderId(orderId);
        e.setOrderNumber("O-" + orderId);
        e.setOrderPrice(10_000L);
        e.setDeleteYn(false);
        LocalDateTime now = LocalDateTime.now();
        e.setPublishedDatetime(now);
        e.setCreatedUserId(1L);
        e.setCreatedUserType("ADMIN");
        e.setCreatedDatetime(now);
        e.setModifiedUserId(1L);
        e.setModifiedUserType("ADMIN");
        e.setModifiedDatetime(now);
        e.setVersion(0L);
        return e;
    }

    @Test
    @DisplayName("신규는 persist, 기존은 merge 로 반영된다")
    @Transactional(transactionManager = "jpaTxManager")
    @Rollback
    void save_persist_and_merge() {
        // persist (ID는 newOrder()에서 TSID로 채워짐)
        OrderEntity e1 = newOrder(null, 1001L);
        orderRepository.save(e1);
        em.flush();

        assertThat(e1.getId()).isNotNull();

        // merge
        e1.setOrderPrice(20_000L);
        orderRepository.save(e1);
        em.flush();
    }

    @Test
    @DisplayName("지정한 주문ID 목록만 삭제한다")
    @Transactional(transactionManager = "jpaTxManager")
    @Rollback
    void delete_selected_ids() {
        // given (ID들은 newOrder()에서 자동 생성)
        OrderEntity e1 = newOrder(null, 2001L);
        OrderEntity e2 = newOrder(null, 2002L);
        OrderEntity e3 = newOrder(null, 2003L);
        orderRepository.save(e1);
        orderRepository.save(e2);
        orderRepository.save(e3);
        em.flush();

        // when
        orderRepository.deleteByOrderIdIn(List.of(2001L, 2003L));
        em.flush();

        // then: 남아 있어야 하는 건 2002L 하나
        Long remain = em.createQuery(
                        "select count(o) from OrderEntity o where o.orderId in (:ids)", Long.class)
                .setParameter("ids", List.of(2001L, 2002L, 2003L))
                .getSingleResult();

        assertThat(remain).isEqualTo(1L);
    }
}
