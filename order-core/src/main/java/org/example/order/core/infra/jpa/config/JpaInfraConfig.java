package org.example.order.core.infra.jpa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 인프라 진입점
 * - jpa.enabled=true 일 때만 활성화
 * - 하위 조립 설정을 명시적으로 Import (전체 스캔 금지)
 * - Spring Data JPA 인터페이스 스캔은 사용하지 않음(커스텀 리포지토리만 조립)
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@ConditionalOnProperty(name = "jpa.enabled", havingValue = "true", matchIfMissing = false)
@Import({
        JpaOrderQueryInfraConfig.class,         // JPAQueryFactory & 조회 리포지토리
        JpaOrderRepositoryInfraConfig.class,    // 저장 리포지토리
        JpaOrderCommandInfraConfig.class        // JDBC 기반 Command 리포지토리
})
public class JpaInfraConfig {
    // 게이트 전용: 빈 정의 없음 (하위 설정에서 모두 제공)
}
