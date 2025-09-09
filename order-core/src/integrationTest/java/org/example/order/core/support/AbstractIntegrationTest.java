package org.example.order.core.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 공통 베이스
 * - Testcontainers MySQL 8.x를 테스트 시작 전에 기동
 * - Spring Boot에 datasource/jpa 관련 프로퍼티를 주입
 * - 각 @SpringBootTest는 이 클래스를 extends 하기만 하면 됨
 */
@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
public abstract class AbstractIntegrationTest {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.36");

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("orderdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startMySql() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        r.add("spring.datasource.hikari.maximum-pool-size", () -> "40");
        r.add("spring.datasource.hikari.minimum-idle", () -> "1");
        r.add("spring.datasource.hikari.connection-timeout", () -> "45000");

        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");

        r.add("spring.jpa.show-sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        r.add("spring.sql.init.mode", () -> "never");
    }
}
