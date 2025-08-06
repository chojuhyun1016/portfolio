package org.example.order.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.s3.config.S3Config;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({OrderCoreConfig.class, KafkaModuleConfig.class, S3Config.class})
@EnableConfigurationProperties(BatchProperties.class)
@ComponentScan(value = {
        "org.example.order.client.kafka",
        "org.example.order.client.s3"
})
public class BatchConfig {
}
