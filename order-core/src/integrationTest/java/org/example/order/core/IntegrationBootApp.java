package org.example.order.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.jpa.config.JpaInfraConfig;
import org.example.order.core.infra.jpa.repository.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.example.order.domain.order.entity.OrderEntity;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 통합 테스트용 부트스트랩(원본)
 * - 락/레디스 관련 스캔 유지 (기존 테스트용)
 * - JPA 관련 자동설정은 제외 (기존 동작 유지)
 */
@SpringBootConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAutoConfiguration(
        exclude = {
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "org.example.order.core.infra.lock",
                "org.example.order.core.infra.redis"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.QuerydslConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\.core\\.infra\\.jpa\\..*")
        }
)
public class IntegrationBootApp {

    /**
     * JPA 전용 미니 컨텍스트 (내부 슬라이스)
     * - 이 클래스는 @EnableAutoConfiguration 를 사용하지 않는다.
     * - 필요한 자동설정만 직접 Import 해서 Redis/Redisson이 절대 로드되지 못하게 한다.
     * - JPA 통합 테스트는 이 클래스를 ApplicationContext 루트로 사용한다.
     */
    @SpringBootConfiguration
    @Import({
            // JDBC & 트랜잭션
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            // JPA
            HibernateJpaAutoConfiguration.class
    })
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackageClasses = OrderEntity.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataOrderJpaRepository.class)
    @ComponentScan(basePackageClasses = JpaInfraConfig.class)
    public static class JpaItSlice {

        @Bean
        public TsidFactory tsidFactory() {
            return TsidFactory.builder().build();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
