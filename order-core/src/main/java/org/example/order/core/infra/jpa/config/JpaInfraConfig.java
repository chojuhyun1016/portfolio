package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.impl.OrderCommandRepositoryJdbcImpl;
import org.example.order.core.infra.persistence.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderQueryRepositoryJpaImpl;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderRepositoryJpaImpl;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.github.f4b6a3.tsid.TsidFactory;

/**
 * JPA / QueryDSL 인프라 단일 진입점 구성.
 * <p>
 * 전역 스위치:
 * - jpa.enabled=true  일 때만 모든 빈이 조건부로 등록됩니다.
 * <p>
 * 등록 정책(조건부):
 * - JPAQueryFactory           : EntityManager 존재 시 등록
 * - OrderCommandRepository    : JdbcTemplate & TsidFactory 존재 시 등록
 * - OrderQueryRepository      : JPAQueryFactory 존재 시 등록
 * - OrderRepository (Adapter) : SpringDataOrderJpaRepository 존재 시 등록
 * <p>
 * 주의:
 * - 라이브러리 레이어에서는 @Component/@Repository를 사용하지 않고, 설정(@Bean)만으로 조립합니다.
 * - OFF(기본) 상태에서는 어떤 빈도 로딩되지 않아 다른 모듈에 영향을 주지 않습니다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "jpa", name = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaInfraConfig {

    /* --------- QueryDSL --------- */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EntityManager.class)
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        log.info("[JpaInfra] Register JPAQueryFactory");

        return new JPAQueryFactory(JPQLTemplates.DEFAULT, em);
    }

    /* --------- Order Command (JDBC) --------- */
    @Bean
    @ConditionalOnMissingBean(OrderCommandRepository.class)
    @ConditionalOnBean({JdbcTemplate.class, TsidFactory.class})
    public OrderCommandRepository orderCommandRepositoryJdbc(JdbcTemplate jdbcTemplate, TsidFactory tsidFactory) {
        log.info("[JpaInfra] Register OrderCommandRepositoryJdbcImpl");

        return new OrderCommandRepositoryJdbcImpl(jdbcTemplate, tsidFactory);
    }

    /* --------- Order Query (QueryDSL JPA) --------- */
    @Bean
    @ConditionalOnMissingBean(OrderQueryRepository.class)
    @ConditionalOnBean(JPAQueryFactory.class)
    public OrderQueryRepository orderQueryRepositoryJpa(JPAQueryFactory queryFactory) {
        log.info("[JpaInfra] Register OrderQueryRepositoryJpaImpl");

        return new OrderQueryRepositoryJpaImpl(queryFactory);
    }

    /* --------- Order Repository (Spring Data JPA 어댑터) --------- */

    @Bean
    @ConditionalOnMissingBean(OrderRepository.class)
    @ConditionalOnBean(SpringDataOrderJpaRepository.class)
    public OrderRepository orderRepositoryJpa(SpringDataOrderJpaRepository jpaRepository) {
        log.info("[JpaInfra] Register OrderRepositoryJpaImpl (adapter of SpringData)");

        return new OrderRepositoryJpaImpl(jpaRepository);
    }
}
