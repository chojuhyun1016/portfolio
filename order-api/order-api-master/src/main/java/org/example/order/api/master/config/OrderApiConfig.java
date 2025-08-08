package org.example.order.api.master.config;

import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OrderCoreConfig.class})
@ComponentScan(value = {
        "org.example.order.client.kafka"
})
public class OrderApiConfig {
}
