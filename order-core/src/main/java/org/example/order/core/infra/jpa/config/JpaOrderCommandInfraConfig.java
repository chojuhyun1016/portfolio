package org.example.order.core.infra.jpa.config;

import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.core.infra.persistence.order.jdbc.impl.OrderCommandRepositoryJdbcImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class JpaOrderCommandInfraConfig {

    @Bean
    @ConditionalOnMissingBean(OrderCommandRepository.class)
    public OrderCommandRepository orderCommandRepository(JdbcTemplate jdbcTemplate) {
        log.info("[JpaInfra-OrderCommand] Register OrderCommandRepositoryJdbcImpl");

        return new OrderCommandRepositoryJdbcImpl(jdbcTemplate);
    }
}
