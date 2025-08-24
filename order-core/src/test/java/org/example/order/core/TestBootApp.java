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

// ✅ 추가 import (기존 주석 유지)
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 테스트 전용 부트 구성 루트 클래스
 *
 * - @SpringBootConfiguration : @Configuration의 상위 부트 메타 구성
 * - @EnableAutoConfiguration : Spring Boot 자동 구성 활성화
 * - @EnableAspectJAutoProxy  : AOP 프록시 생성 (Aspect 테스트용)
 * - @ComponentScan("org.example.order.core")
 *      → core 하위의 @Component/@Service/@Configuration 들을 테스트 컨텍스트에 포함
 *
 * ※ 이 클래스를 테스트의 기준 구성(classes=...) 으로 지정하면,
 *    IDE/빌드 모두에서 @Autowired 빈 인식 문제가 사라집니다.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ⚠️ test-unit 프로필(유닛 테스트)에서는 JPA/리포지토리/마이그레이션 자동구성이 절대 필요 없습니다.
 *    아래 exclude 들로 확실히 차단하고, 스캔에서도 infra.jpa / infra.querydsl 를 제외합니다.
 *    이렇게 해야 @PersistenceContext 가 붙은 사용자 구성(QuerydslConfig 등)이 로드되지 않고,
 *    EntityManagerFactory 미존재 예외(NoSuchBeanDefinitionException)가 발생하지 않습니다.
 * ──────────────────────────────────────────────────────────────────────────
 *
 * ✅ [중요] 이 클래스는 **src/test/java**(유닛 테스트 전용) 에만 존재합니다.
 *    통합 테스트 클래스패스에는 포함되지 않으므로 @SpringBootConfiguration 충돌이 발생하지 않습니다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(
        exclude = {
                // ✅ JPA/데이터소스/리포지토리/마이그레이션 자동구성 제거
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
// 아래 ImportAutoConfiguration 은 가독성 차원에서 명시(EnableAutoConfiguration의 exclude가 이미 충분하지만, 의도를 분명히 함)
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
// ✅ 스캔 범위를 lock 패키지로 “축소”해서 JPA/Repository/Config 들이 아예 들어오지 않게 함
@ComponentScan(
        basePackages = "org.example.order.core.infra.lock",
        excludeFilters = {
                // ✅ 혹시라도 경로가 다른 곳으로 이동해 온 QuerydslConfig 를 이름 기준으로 싹 제외
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.QuerydslConfig"),
                // ✅ 혹시라도 추가로 jpa 패키지가 들어오는 상황에 대한 2차 방어
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\.core\\.infra\\.jpa\\..*")
        }
)
public class TestBootApp {
    // 빈 정의 없음: 스캔 & 자동구성에만 의존

    // ───────────────────────────────────────────────────────────────
    // ✅ 추가 방어선 (test-unit 프로필 한정):
    //    QuerydslConfig 에 @PersistenceContext 가 존재해도, 여기서 더미 EMF/EM/SharedEM 을 제공하여
    //    PersistenceAnnotationBeanPostProcessor 가 실패하지 않도록 한다.
    //    → 어떤 경로(@Import 등)로 QuerydslConfig/JPA Repo 가 유입돼도 NPE/NoSuchBeanDefinitionException 차단
    // ───────────────────────────────────────────────────────────────

    // ※ 이번 오류는 Spring Data JPA가 'jpaSharedEM_entityManagerFactory' 이름의 공유 EM 빈을 찾다가
    //    내부적으로 'entityManagerFactory' 빈을 요구하는 과정에서 실패했습니다.
    //    따라서 정확한 “빈 이름”으로 등록해 줘야 순환이 완전히 끊깁니다.

    @Bean(name = "entityManagerFactory")
    @Profile("test-unit")
    public EntityManagerFactory testEntityManagerFactory() {
        // org.mockito.Mockito 를 사용 (test 스코프)
        EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
        EntityManager em = Mockito.mock(EntityManager.class);
        Mockito.when(emf.createEntityManager()).thenReturn(em);
        return emf;
    }

    @Bean(name = "jpaSharedEM_entityManagerFactory")
    @Profile("test-unit")
    public EntityManager jpaSharedEntityManager(EntityManagerFactory emf) {
        // Spring이 JPA 자동구성 시 만들어주는 공유 EM 빈 이름과 타입을 그대로 맞춰줌
        // 실제 DB 접근은 없고, 더미 EMF 기반 프록시 EM 이므로 안전
        return SharedEntityManagerCreator.createSharedEntityManager(emf);
    }

    // 필요 시 직접 EM 을 주입받는 곳을 위해 노멀 EM 도 노출(빈 이름 강제는 필요치 않음)
    @Bean
    @Profile("test-unit")
    public EntityManager testEntityManager(EntityManagerFactory emf) {
        return emf.createEntityManager();
    }

    // ───────────────────────────────────────────────────────────────
    // ✅ JDBC 안전판: 어떤 구성(@Import/컴포넌트스캔) 경로로 JDBC 레이어 빈이 유입돼도
    //    컨텍스트 로딩 실패 방지. DB 자동구성은 exclude 상태이므로 실제 연결은 절대 일어나지 않음.
    // ───────────────────────────────────────────────────────────────

    @Bean
    @Profile("test-unit")
    public DataSource testDataSource() {
        // DataSource 을 요구하는 빈 대비 더미로 스텁
        return Mockito.mock(DataSource.class);
    }

    @Bean
    @Profile("test-unit")
    public JdbcTemplate testJdbcTemplate(DataSource ds) {
        // JdbcTemplate 자체는 DataSource 를 사용하지만,
        // 위에서 더미 DataSource 를 주입하므로 실제 커넥션은 발생하지 않음
        return new JdbcTemplate(ds);
    }

    @Bean
    @Profile("test-unit")
    public NamedParameterJdbcTemplate testNamedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    // (선택) 트랜잭션 관련 빈을 참조하는 경우를 대비한 얇은 스텁
    @Bean
    @Profile("test-unit")
    public PlatformTransactionManager testTxManager() {
        return Mockito.mock(PlatformTransactionManager.class);
    }
}
