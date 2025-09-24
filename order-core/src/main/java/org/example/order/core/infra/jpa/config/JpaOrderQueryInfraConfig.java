package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderQueryRepositoryJpaImpl;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Querydsl 기반 '조회' 리포지토리 조립 설정
 * <p>
 * 변경 사항
 * - (중요) 여기서는 조회 인프라만 담당합니다.
 * - JPAQueryFactory는 이곳에서 단일로 제공(보조). 중복 제공 제거.
 * - 상위 JpaInfraConfig(jpa.enabled=true)에서만 Import 되도록 게이트 뒤에 존재.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
public class JpaOrderQueryInfraConfig {

    @Bean
    @ConditionalOnMissingBean(JPAQueryFactory.class)
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }

    @Bean
    @ConditionalOnMissingBean(OrderQueryRepository.class)
    public OrderQueryRepository orderQueryRepository(JPAQueryFactory queryFactory) {
        log.info("[JpaInfra-OrderQuery] Register OrderQueryRepositoryJpaImpl");

        return new OrderQueryRepositoryJpaImpl(queryFactory);
    }
}
