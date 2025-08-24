package org.example.order.core;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * 통합 테스트 전용 Boot 루트
 *
 * - integrationTest 클래스패스에만 존재(@SpringBootConfiguration)
 * - DataSource 자동구성은 허용(컨테이너 MySQL을 사용)
 * - JPA/Repo/Flyway/Liquibase는 제외 (락/레디슨 통합테스트에 불필요)
 * - 스캔을 infra.lock / infra.redis 로 한정 → QuerydslConfig 등 유입 차단
 *
 * ✅ 이 클래스를 별도 파일로 둠으로써, test 산출물(TestBootApp.class)을
 *    integrationTest 클래스패스에서 제외했을 때 @SpringBootConfiguration이
 *    “단 하나”만 존재하도록 보장한다. (중복 부트루트 충돌 원천 차단)
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
@EnableAspectJAutoProxy(proxyTargetClass = true)
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
    // 빈 정의 없음 (필요한 건 각 테스트의 @DynamicPropertySource로 주입)
}
