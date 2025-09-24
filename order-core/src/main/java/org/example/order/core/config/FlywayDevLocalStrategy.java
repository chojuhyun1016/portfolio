package org.example.order.core.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * FlywayRepairOnLocal
 * - local, dev 프로필에서만 repair 후 migrate 수행
 * - 실패 이력/체크섬 불일치 정리
 */
@Slf4j
@Configuration
@Profile({"local", "dev"})
public class FlywayDevLocalStrategy {

    @Bean
    public FlywayMigrationStrategy devLocalFlywayStrategy() {
        return (Flyway flyway) -> {
            try {
                log.info("[Flyway] dev/local: migrate start");

                flyway.migrate();

                log.info("[Flyway] dev/local: migrate success");
            } catch (FlywayValidateException ve) { // ⬅️ 검증 실패일 때만 repair
                log.warn("[Flyway] dev/local: validation failed → repair once: {}", ve.getMessage());

                flyway.repair();

                log.info("[Flyway] dev/local: repair done → migrate retry");

                flyway.migrate();

                log.info("[Flyway] dev/local: migrate success after repair");
            } catch (FlywayException e) {
                log.error("[Flyway] dev/local: migrate failed (non-validation): {}", e.getMessage(), e);

                throw e;
            }
        };
    }
}
