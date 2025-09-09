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
     * JPA 전용 슬라이스
     * - 필요한 자동설정만 가져오고, 레포 조립 설정(JpaInfraConfig)을 직접 import
     * - 다른 통합테스트 컨텍스트와 분리 (영향 없음)
     */
    @SpringBootConfiguration
    @Import({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,

            JpaInfraConfig.class
    })
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = OrderEntity.class)
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
