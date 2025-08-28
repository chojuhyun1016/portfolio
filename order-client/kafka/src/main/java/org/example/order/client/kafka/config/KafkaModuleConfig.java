package org.example.order.client.kafka.config;

import org.example.order.client.kafka.config.consumer.KafkaConsumerConfig;
import org.example.order.client.kafka.config.producer.KafkaProducerConfig;
import org.example.order.client.kafka.config.topic.KafkaTopicConfig;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 모듈 조립 + 서비스 빈 조건부 등록 (단일 설정 클래스)
 * <p>
 * - Consumer/Producer/Topic 바인딩 설정은 @Import 로 가져오되,
 * 각 설정 클래스 내부의 @ConditionalOnProperty 에 의해 개별 온오프.
 * <p>
 * - Producer 서비스(KafkaProducerCluster)도 여기서 조건부 @Bean 으로 등록:
 * 1) kafka.producer.enabled=true
 * 2) KafkaTemplate 빈 존재 (@ConditionalOnBean)
 * <p>
 * 이렇게 하면 infra 모듈은 스캔할 필요 없고, 애플리케이션에서
 * 이 클래스 하나만 @Import 하면 전체가 조립됩니다.
 */
@Configuration
@Import({
        KafkaConsumerConfig.class,   // kafka.consumer.enabled=true 일 때만 활성화
        KafkaProducerConfig.class,   // kafka.producer.enabled=true 일 때만 활성화
        KafkaTopicConfig.class       // topic 바인딩만 담당 (빈 없음)
})
public class KafkaModuleConfig {

    /**
     * Producer 서비스 빈 (SmartLifecycle)
     * - producer.enabled=true 이고 KafkaTemplate 이 빈으로 존재할 때만 등록
     */
    @Bean
    @ConditionalOnProperty(prefix = "kafka.producer", name = "enabled", havingValue = "true")
    @ConditionalOnBean(KafkaTemplate.class)
    public KafkaProducerCluster kafkaProducerCluster(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaProducerCluster(kafkaTemplate);
    }
}
