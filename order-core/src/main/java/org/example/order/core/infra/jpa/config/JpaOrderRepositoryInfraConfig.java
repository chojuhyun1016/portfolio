package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderRepositoryJpaImpl;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Order Repository(JPA + Querydsl) 조립 설정 (저장/기본 CRUD)
 * <p>
 * 주의:
 * - 상위 JpaInfraConfig 에서만 @Import 되어 활성화됩니다.
 * - 토글은 상위의 @ConditionalOnProperty(jpa.enabled=true)로만 관리합니다.
 * - 조건 경합을 막기 위해 @ConditionalOnBean(EntityManager...)와 같은 조건은 두지 않습니다.
 * → 필요한 빈(JPAQueryFactory, EntityManager)은 파라미터 주입으로 의존관계를 분명히 합니다.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
public class JpaOrderRepositoryInfraConfig {

    @Bean
    @ConditionalOnMissingBean(OrderRepository.class)
    public OrderRepository orderRepositoryJpa(JPAQueryFactory queryFactory, EntityManager em) {
        log.info("[JpaInfra-OrderRepository] Register OrderRepositoryJpaImpl");

        return new OrderRepositoryJpaImpl(queryFactory, em);
    }
}
