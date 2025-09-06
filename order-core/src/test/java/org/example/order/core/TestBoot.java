package org.example.order.core;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootConfiguration
@EnableAutoConfiguration(
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
@ImportAutoConfiguration(
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(
        basePackages = "org.example.order.core.infra.lock",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.QuerydslConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\.core\\.infra\\.jpa\\..*")
        }
)
public class TestBoot {
    @Bean(name = "entityManagerFactory")
    @Profile("test-unit")
    public EntityManagerFactory testEntityManagerFactory() {
        EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
        EntityManager em = Mockito.mock(EntityManager.class);
        Mockito.when(emf.createEntityManager()).thenReturn(em);

        return emf;
    }

    @Bean(name = "jpaSharedEM_entityManagerFactory")
    @Profile("test-unit")
    public EntityManager jpaSharedEntityManager(EntityManagerFactory emf) {
        return SharedEntityManagerCreator.createSharedEntityManager(emf);
    }

    @Bean
    @Profile("test-unit")
    public EntityManager testEntityManager(EntityManagerFactory emf) {
        return emf.createEntityManager();
    }

    @Bean
    @Profile("test-unit")
    public DataSource testDataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    @Profile("test-unit")
    public JdbcTemplate testJdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    @Profile("test-unit")
    public NamedParameterJdbcTemplate testNamedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    @Profile("test-unit")
    public PlatformTransactionManager testTxManager() {
        return Mockito.mock(PlatformTransactionManager.class);
    }
}
