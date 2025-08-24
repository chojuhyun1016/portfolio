package org.example.order.core.infra.lock.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL(Testcontainers) 통합 테스트 공통 베이스
 * - namedLock 사용을 위해 spring.datasource.* + lock.named.enabled 동적 주입
 * <p>
 * 주의:
 * - 프로덕션 코드는 변경하지 않는다.
 */
@Testcontainers
public abstract class MysqlContainerSupport {

    @Container
    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL =
            // ✅ 공식 MySQL 8 이미지 사용
            new MySQLContainer<>("mysql:8.0.36")
                    .withUsername("test")
                    .withPassword("test")
                    .withDatabaseName("orderdb");

    @DynamicPropertySource
    static void registerMysqlProps(DynamicPropertyRegistry registry) {
        registry.add("lock.enabled", () -> "true");
        registry.add("lock.redisson.enabled", () -> "false");
        registry.add("lock.named.enabled", () -> "true");

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver"); // ✅ 최신 드라이버 명시

        // Hikari 기본 풀 설정(필수는 아님, 안정성 보강)
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "4");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");

        // DDL 자동 생성 끄기(락만 사용)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeAll
    static void beforeAll() {
        MYSQL.start();
    }

    @AfterAll
    static void afterAll() {
        MYSQL.stop();
    }
}
