package org.example.order.core.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 통합 테스트 공통 베이스 (MySQL).
 *
 * - mysql:8.0.36 컨테이너를 선기동하고, Spring Boot DataSource 프로퍼티를 동적으로 주입한다.
 * - 이 클래스를 상속하면 @SpringBootTest 가 뜨기 전에 URL/USER/PW/DRIVER 가 항상 셋업된다.
 * - DataSource 자동설정 배제(exclude) 금지.
 */
public abstract class AbstractIntegrationTest {

    public static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.36")
                    .withUsername("test")
                    .withPassword("test")
                    .withDatabaseName("testdb");

    @BeforeAll
    static void startContainers() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
    }

    @AfterAll
    static void stopContainers() {
        // 로컬 캐시 재사용 시에는 stop 생략 가능
        // MYSQL.stop();
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        // DataSource (부트 자동구성을 위한 표준 키)
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // JPA (원하는대로 조정)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }
}
