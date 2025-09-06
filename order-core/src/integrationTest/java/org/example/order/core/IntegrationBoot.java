package org.example.order.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.jpa.config.JpaInfraConfig;
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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 통합 테스트용 부트스트랩 (필요 최소 변경만 반영)
 * - 기존 락/레디스 스캔 유지
 * - 메인 컨텍스트에서는 JPA 자동설정 제외(기존 테스트 영향 최소화)
 * - JPA IT는 내부 슬라이스(JpaItSlice)만 사용
 */
@SpringBootConfiguration
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
public class IntegrationBoot {

    /**
     * JPA 전용 슬라이스 (테스트에서 classes=IntegrationBoot.JpaItSlice.class 로 사용)
     * - 존재하지 않는 adapter 클래스를 참조하지 않고, 패키지 문자열 스캔으로 대체
     * - 트랜잭션매니저 커스텀 빈 생성 안 함(기존 Override 예외 방지)
     * - 레디스/레디슨 자동설정은 가져오지 않음
     */
    @SpringBootConfiguration
    @Import({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = OrderEntity.class)
    @EnableJpaRepositories(
            // ★ 리포지토리 인터페이스들이 들어있는 실제 패키지 루트로 맞춰 주세요.
            //   (예: org.example.order.core.infra.persistence.order.jpa.repository 인 경우 그 경로)
            basePackages = "org.example.order.core.infra.persistence.order.jpa"
    )
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
