package org.example.order.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.s3.config.S3ModuleConfig;
import org.example.order.client.web.config.WebClientModuleConfig;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Order Worker 구성 루트
 * - core/infra, client(web/kafka/s3) 모듈은 설정 기반(@Bean) Import
 * - Worker 자체 패키지만 ComponentScan
 */
@Configuration
@Import({
        OrderCoreConfig.class,
        KafkaModuleConfig.class,
        S3ModuleConfig.class,
        WebClientModuleConfig.class
})
@ComponentScan(basePackages = {
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.controller",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle"
})
@RequiredArgsConstructor
public class OrderWorkerConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
