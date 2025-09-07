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

    // 고정 이미지 태그 사용 권장 (예: 8.0.36)
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.36");

    // 재사용을 위한 static 컨테이너
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("orderdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startMySql() {
        // 컨테이너가 아직 안 떠있으면 기동
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
    }

    /**
     * Spring 컨텍스트에 동적으로 데이터소스/하이버네이트 설정 주입
     */
    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        // DataSource
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Hikari (테스트에서 타임아웃 줄이기)
        r.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        r.add("spring.datasource.hikari.minimum-idle", () -> "1");
        r.add("spring.datasource.hikari.connection-timeout", () -> "25000");

        // JPA / Hibernate
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop"); // 각 IT에서 이미 주는 경우 있어도 여기서 보강

        // SQL 로깅(원하면 켜세요)
        r.add("spring.jpa.show-sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // 스키마 초기화 관련(필요시)
        r.add("spring.sql.init.mode", () -> "never");
    }
}
