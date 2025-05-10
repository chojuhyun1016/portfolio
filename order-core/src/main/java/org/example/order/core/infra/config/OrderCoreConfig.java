package org.example.order.core.infra.config;

import org.example.order.core.infra.jpa.config.QuerydslConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "org.example.order.core",
        "org.example.order.common"
})
@EntityScan(value = {"org.example.order.core.domain"})
@EnableJpaRepositories(value = {"org.example.order.core.infra.jpa.repository"})
@Import({QuerydslConfig.class})
public class OrderCoreConfig {
}
