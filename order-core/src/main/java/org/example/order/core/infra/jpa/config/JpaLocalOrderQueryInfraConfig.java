package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jpa.impl.LocalOrderQueryRepositoryJpaImpl;
import org.example.order.domain.order.repository.LocalOrderQueryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Querydsl 기반 '조회' 리포지토리 조립 설정 (local_order)
 * - JPAQueryFactory는 JpaOrderQueryInfraConfig 에서 제공됨 (ConditionalOnMissingBean)
 * - 여기서는 LocalOrder 전용 조회 리포지토리를 등록
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
public class JpaLocalOrderQueryInfraConfig {

    @Bean
    @ConditionalOnMissingBean(LocalOrderQueryRepository.class)
    public LocalOrderQueryRepository localOrderQueryRepository(JPAQueryFactory queryFactory) {
        log.info("[JpaInfra-LocalOrderQuery] Register LocalOrderQueryRepositoryJpaImpl");

        return new LocalOrderQueryRepositoryJpaImpl(queryFactory);
    }
}
