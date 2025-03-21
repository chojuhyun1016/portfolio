package org.example.order.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.config.KafkaConfig;
import org.example.order.common.config.module.CommonObjectMapperFactory;
import org.example.order.core.config.OrderCoreConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OrderCoreConfig.class, KafkaConfig.class})
@ComponentScan(value = {
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.controller",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle",
        "org.example.order.client.web",
        "org.example.order.client.kafka",
        "org.example.order.client.s3"
})
@RequiredArgsConstructor
public class OrderWorkerConfig {

    @Bean
    ObjectMapper objectMapper() {
        return CommonObjectMapperFactory.defaultObjectMapper();
    }
}
