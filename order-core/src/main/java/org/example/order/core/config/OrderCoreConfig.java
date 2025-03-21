package org.example.order.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(value = {"org.example.order.core.domain"})
@Import({QuerydslConfig.class})
@EnableJpaRepositories(value = {"org.example.order.core.repository"})
public class OrderCoreConfig {
}
