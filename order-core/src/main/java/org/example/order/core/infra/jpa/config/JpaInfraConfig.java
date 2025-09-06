package org.example.order.core.infra.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * JPA / QueryDSL 기본 인프라 설정 (핵심 + 하위 조립 설정 일괄 임포트)
 * <p>
 * 전역 스위치:
 * - jpa.enabled=true 일 때만 조건부 등록
 * <p>
 * 등록 정책(조건부):
 * - JPAQueryFactory : EntityManager 존재 시 등록
 * - 하위 설정(JpaOrder*InfraConfig)들은 @Import로 일괄 포함되어, jpa.enabled 조건 하에서만 활성화
 * <p>
 * 주의:
 * - 레포지토리 조립(명령/조회/JPA 저장소)은 별도 설정 클래스로 유지하고, 여기서 한 번에 Import 합니다.
 * - 최상위 OrderCoreConfig 에서는 JpaInfraConfig 하나만 Import 하면 됩니다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "jpa", name = "enabled", havingValue = "true", matchIfMissing = false)
@Import({
        JpaOrderCommandInfraConfig.class,
        JpaOrderQueryInfraConfig.class,
        JpaOrderRepositoryInfraConfig.class
})
public class JpaInfraConfig {

    /* --------- QueryDSL (Core) --------- */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EntityManager.class)
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        log.info("[JpaInfra] Register JPAQueryFactory");

        return new JPAQueryFactory(em);
    }
}
