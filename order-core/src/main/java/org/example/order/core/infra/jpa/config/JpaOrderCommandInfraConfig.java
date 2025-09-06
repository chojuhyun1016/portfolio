package org.example.order.core.infra.jpa.config;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jdbc.impl.OrderCommandRepositoryJdbcImpl;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Order Command(JDBC) 조립 설정
 * <p>
 * 주의:
 * - 이 설정은 상위 JpaInfraConfig 에서만 @Import 되어 활성화됩니다.
 * - 토글은 상위(JpaInfraConfig)의 @ConditionalOnProperty 에서만 관리합니다.
 */
@Slf4j
@Configuration
public class JpaOrderCommandInfraConfig {

    @Bean
    @ConditionalOnMissingBean(OrderCommandRepository.class)
    @ConditionalOnBean({JdbcTemplate.class, TsidFactory.class})
    public OrderCommandRepository orderCommandRepositoryJdbc(JdbcTemplate jdbcTemplate, TsidFactory tsidFactory) {
        log.info("[JpaInfra-OrderCommand] Register OrderCommandRepositoryJdbcImpl");

        return new OrderCommandRepositoryJdbcImpl(jdbcTemplate, tsidFactory);
    }
}
