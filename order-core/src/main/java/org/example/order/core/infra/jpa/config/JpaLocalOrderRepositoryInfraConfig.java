package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jpa.impl.LocalOrderRepositoryJpaImpl;
import org.example.order.domain.order.repository.LocalOrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LocalOrder Repository(JPA + Querydsl) 조립 설정 (저장/기본 CRUD)
 * - JpaInfraConfig 에서 Import 되어 활성화
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
public class JpaLocalOrderRepositoryInfraConfig {

    @Bean
    @ConditionalOnMissingBean(LocalOrderRepository.class)
    public LocalOrderRepository localOrderRepositoryJpa(JPAQueryFactory queryFactory, EntityManager em) {
        log.info("[JpaInfra-LocalOrderRepository] Register LocalOrderRepositoryJpaImpl");

        return new LocalOrderRepositoryJpaImpl(queryFactory, em);
    }
}
