package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderRepositoryJpaImpl;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Order Repository(JPA + Querydsl) 조립 설정
 * <p>
 * 주의:
 * - 이 설정은 상위 JpaInfraConfig 에서만 @Import 되어 활성화됩니다.
 * - 토글은 상위(JpaInfraConfig)의 @ConditionalOnProperty 에서만 관리합니다.
 */
@Slf4j
@Configuration
public class JpaOrderRepositoryInfraConfig {

    @Bean
    @ConditionalOnMissingBean(OrderRepository.class)
    @ConditionalOnBean({JPAQueryFactory.class, EntityManager.class})
    public OrderRepository orderRepositoryJpa(JPAQueryFactory queryFactory, EntityManager em) {
        log.info("[JpaInfra-OrderRepository] Register OrderRepositoryJpaImpl");

        return new OrderRepositoryJpaImpl(queryFactory, em);
    }
}
