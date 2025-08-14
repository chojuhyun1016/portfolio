package org.example.order.core.infra.lock.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * 테스트 전용 MySQL Testcontainers 설정
 * - NamedLock(MySQL GET_LOCK/RELEASE_LOCK) 검증용
 * - @ActiveProfiles("test") 에서만 활성화
 */
@TestConfiguration
@Profile("test")
public class TestMySqlConfig {

    /**
     * MySQL 컨테이너 (8.0 계열)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
                .withUsername("test")
                .withPassword("test")
                .withDatabaseName("testdb");
    }

    /**
     * Hikari DataSource (destroy 시 close 호출 보장)
     */
    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(JdbcDatabaseContainer<?> mysql) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(mysql.getJdbcUrl());
        ds.setUsername(mysql.getUsername());
        ds.setPassword(mysql.getPassword());
        ds.setMaximumPoolSize(5);
        return ds;
    }
}
