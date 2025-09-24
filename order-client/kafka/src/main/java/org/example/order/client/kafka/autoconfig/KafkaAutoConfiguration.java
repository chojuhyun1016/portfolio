package org.example.order.client.kafka.autoconfig;

import org.example.order.client.kafka.config.consumer.KafkaConsumerConfig;
import org.example.order.client.kafka.config.producer.KafkaProducerConfig;
import org.example.order.client.kafka.config.topic.KafkaTopicConfig;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * KafkaAutoConfiguration
 * - web/S3와 동일한 자동구성 패턴
 * - 내부 구성(KafkaConsumerConfig / KafkaProducerConfig / KafkaTopicConfig)을 import
 * - 프로듀서 서비스(KafkaProducerCluster)는 프로퍼티/빈 조건 충족 시에만 등록
 */
@AutoConfiguration
@Import({
        KafkaConsumerConfig.class,
        KafkaProducerConfig.class,
        KafkaTopicConfig.class
})
public class KafkaAutoConfiguration {

    /**
     * Producer 서비스 빈 (SmartLifecycle)
     * - producer.enabled=true 이고 KafkaTemplate 빈이 존재할 때만 등록
     */
    @Bean
    @ConditionalOnProperty(prefix = "kafka.producer", name = "enabled", havingValue = "true")
    @ConditionalOnBean(KafkaTemplate.class)
    public KafkaProducerCluster kafkaProducerCluster(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaProducerCluster(kafkaTemplate);
    }
}
