package org.example.order.api.web.config;

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
 * ------------------------------------------------------------------------
 * 목적
 * - worker/API master 스타일로 심플 구성
 * - Core 구성 명시 Import
 * - Kafka 자동구성 사용(프로퍼티 조건으로 on/off)
 * - CorrelationIdFilter / @Correlate Aspect / TaskDecorator 등은 order-common 오토컨피그 사용
 * - ObjectMapper, TopicProperties 등 공통 Bean 정의
 */
@Configuration(proxyBeanMethods = false)
@Import({
        OrderCoreConfig.class
})
@ImportAutoConfiguration({
        KafkaAutoConfiguration.class
})
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ComponentScan(basePackages = {
        "org.example.order.api.web"
})
public class OrderApiWebConfig {

    /**
     * 공통 ObjectMapper (없으면 기본 제공)
     * - Jackson 모듈 자동 등록
     * - JavaTimeModule 등 공통 직렬화/역직렬화 설정
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
