package org.example.order.core.infra.persistence.order.support;

import com.github.f4b6a3.tsid.TsidFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.order.core.infra.persistence.order.jdbc.impl.OrderCommandRepositoryJdbcImpl;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderQueryRepositoryJpaImpl;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderRepositoryJpaImpl;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 인프라 통합테스트 전용 구성
 *
 * 책임 분리:
 * - DataSource / Testcontainers / 동적 프로퍼티: AbstractIntegrationTest 가 담당
 * - TSID(TsidFactory): IntegrationBoot.JpaItSlice 가 담당
 * - 이 클래스는 Repository 조립과 JPAQueryFactory 만 제공
 *
 * ❗ 여기서는 트랜잭션 매니저를 등록하지 않는다.
 *    (JDBC 테스트와 충돌하므로 각 JPA IT 내부 TestConfiguration에서만 등록)
 */
@Configuration
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
public class OrderInfraTestConfig {

    /* ---------- JPA / QueryDSL ---------- */
    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }

    /* ---------- Repository Beans ---------- */
    @Bean
    public OrderQueryRepository orderQueryRepository(JPAQueryFactory queryFactory) {
        return new OrderQueryRepositoryJpaImpl(queryFactory);
    }

    @Bean
    public OrderRepository orderRepository(JPAQueryFactory queryFactory) {
        // @PersistenceContext 필드 em 사용
        return new OrderRepositoryJpaImpl(queryFactory, em);
    }

    @Bean
    public OrderCommandRepository orderCommandRepository(JdbcTemplate jdbcTemplate, TsidFactory tsidFactory) {
        return new OrderCommandRepositoryJdbcImpl(jdbcTemplate, tsidFactory);
    }
}
