package org.example.order.core.infra.jpa.config;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.jdbc.impl.LocalOrderCommandRepositoryJdbcImpl;
import org.example.order.domain.order.repository.LocalOrderCommandRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * LocalOrder Command(JDBC) 리포지토리 조립 설정
 * - JpaInfraConfig 게이트 안에서 Import 되어 활성화됨
 * - 기존 OrderCommandRepository 와 동일한 패턴
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JpaLocalOrderCommandInfraConfig {

    @Bean
    @ConditionalOnMissingBean(LocalOrderCommandRepository.class)
    public LocalOrderCommandRepository localOrderCommandRepository(JdbcTemplate jdbcTemplate) {
        log.info("[JpaInfra-LocalOrderCommand] Register LocalOrderCommandRepositoryJdbcImpl");

        return new LocalOrderCommandRepositoryJdbcImpl(jdbcTemplate);
    }
}
