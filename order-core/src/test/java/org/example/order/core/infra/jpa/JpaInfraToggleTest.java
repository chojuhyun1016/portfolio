package org.example.order.core.infra.jpa;

import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.jpa.config.JpaInfraConfig;
import org.example.order.core.infra.jpa.repository.order.jdbc.impl.OrderCommandRepositoryJdbcImpl;
import org.example.order.core.infra.jpa.repository.order.jpa.adapter.SpringDataOrderJpaRepository;
import org.example.order.core.infra.jpa.repository.order.jpa.impl.OrderQueryRepositoryJpaImpl;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JpaInfraConfig 전역 스위치(jpa.enabled)와 조건부 조립을 JVM 내에서 검증.
 * <p>
 * 시나리오
 * - OFF: 어떤 빈도 로딩되지 않음
 * - ON + 최소 JDBC (H2 DataSource + JdbcTemplate + TsidFactory + JDBC 구현체 @Import):
 * CommandRepo만 로딩
 * - ON + H2 + JPA 오토컨피그 + JPA 구현체 @Import:
 * Command/Query/Repository 모두 로딩
 */
class JpaInfraToggleTest {

    @Test
    void when_disabled_then_no_beans_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues("jpa.enabled=false")
                .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OrderCommandRepository.class);
                    assertThat(ctx).doesNotHaveBean(OrderQueryRepository.class);
                    assertThat(ctx).doesNotHaveBean(OrderRepository.class);
                });
    }

    /**
     * 최소 JDBC 환경:
     * - H2 DataSource + JdbcTemplate 자동구성
     * - TsidFactory 제공
     * - JDBC 구현체를 @Import 로 명시 ★ (ApplicationContextRunner는 컴포넌트 스캔을 안 돌림)
     */
    @Test
    void when_enabled_with_minimal_jdbc_then_only_command_repo_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "jpa.enabled=true",
                        "spring.datasource.url=jdbc:h2:mem:jpa_jdbc_only;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa"
                )
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class
                ))
                .withUserConfiguration(JdbcOnlyBoot.class)
                .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OrderCommandRepository.class);
                    assertThat(ctx).doesNotHaveBean(OrderQueryRepository.class);
                    assertThat(ctx).doesNotHaveBean(OrderRepository.class);
                });
    }

    /**
     * H2 + JPA 오토컨피그:
     * - 엔티티/리포지토리 스캔 + QueryDSL 기반 구현체 @Import
     * - JDBC 구현체 @Import 도 함께 넣어 CommandRepo 조건 충족
     */
    @Test
    void when_enabled_with_h2_and_jpa_autoconfig_then_all_repos_loaded() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "jpa.enabled=true",
                        "spring.datasource.url=jdbc:h2:mem:jpa_full;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "spring.jpa.open-in-view=false"
                )
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        TransactionManagerCustomizationAutoConfiguration.class,
                        JtaAutoConfiguration.class,
                        HibernateJpaAutoConfiguration.class,
                        JpaRepositoriesAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class
                ))
                .withUserConfiguration(H2JpaBoot.class)
                .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
                .run(ctx -> {
                    // JDBC 기반
                    assertThat(ctx).hasSingleBean(OrderCommandRepository.class);
                    // JPA 기반
                    assertThat(ctx).hasSingleBean(OrderQueryRepository.class);
                    assertThat(ctx).hasSingleBean(OrderRepository.class);
                });
    }

    // ----------------------------------------------------------------------
    // 보조 구성
    // ----------------------------------------------------------------------

    /**
     * 최소 JDBC 부트:
     * - TsidFactory 제공
     * - JDBC 구현체 직접 @Import ★
     */
    @Import(OrderCommandRepositoryJdbcImpl.class)
    static class JdbcOnlyBoot {
        @Bean
        TsidFactory tsidFactory() {
            return TsidFactory.builder().build();
        }
    }

    /**
     * H2 + JPA 자동구성 부트:
     * - 엔티티/리포지토리 스캔
     * - TsidFactory 제공
     * - JPA Query 구현체 + JDBC 구현체 @Import ★
     * (스캔이 아닌 구성기반 조립을 테스트 컨텍스트에서 확실히 보장)
     */
    @EntityScan(basePackageClasses = OrderEntity.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataOrderJpaRepository.class)
    @Import({OrderQueryRepositoryJpaImpl.class, OrderCommandRepositoryJdbcImpl.class})
    static class H2JpaBoot {
        @Bean
        TsidFactory tsidFactory() {
            return TsidFactory.builder().build();
        }
    }
}
