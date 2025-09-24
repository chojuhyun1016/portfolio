package com.example.order.api.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

/**
 * OrderApiWebConfig
 * - Core 구성은 명시 Import
 * - Kafka는 자동구성 경로 사용(프로퍼티 조건으로 on/off)
 */
@Configuration(proxyBeanMethods = false)
@Import({
        OrderCoreConfig.class
})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ComponentScan(basePackages = {
        "com.example.order.api.web"
})
public class OrderApiWebConfig {

    /**
     * 공통 ObjectMapper (없으면 기본 제공)
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
