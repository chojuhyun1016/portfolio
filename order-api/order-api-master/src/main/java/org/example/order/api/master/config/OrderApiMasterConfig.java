package org.example.order.api.master.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.client.kafka.config.KafkaModuleConfig;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

/**
 * OrderApiMasterConfig
 * ------------------------------------------------------------------------
 * 목적
 * - API Master 모듈이 의존하는 Core, Kafka 모듈 설정을 Import
 * - ObjectMapper, TopicProperties 등 공통 Bean 정의
 * <p>
 * 특징
 * - proxyBeanMethods=false : CGLIB 프록시 최소화
 * - @Import : OrderCoreConfig, KafkaModuleConfig 직접 포함
 * - @EnableConfigurationProperties : KafkaTopicProperties 활성화
 * - @ComponentScan : API Master 하위 패키지 스캔
 */
@Configuration(proxyBeanMethods = false)
@Import({
        OrderCoreConfig.class,
        KafkaModuleConfig.class
})
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ComponentScan(basePackages = {
        "org.example.order.api.master"
})
public class OrderApiMasterConfig {

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
