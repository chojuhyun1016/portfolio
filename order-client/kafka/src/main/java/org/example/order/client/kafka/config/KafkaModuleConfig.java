package org.example.order.client.kafka.config;

import org.example.order.client.kafka.config.consumer.KafkaConsumerConfig;
import org.example.order.client.kafka.config.producer.KafkaProducerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 큰 맥락
 * - Kafka 모듈 전체 구성을 한 번에 Import 하는 조정 클래스.
 * - ConsumerConfig, ProducerConfig 를 통합적으로 로드한다.
 * - 각 Config 클래스들은 @ConditionalOnProperty 로 개별 활성화 여부가 결정되므로,
 * 이 클래스 자체는 Import 용도로만 존재한다.
 */
@Configuration
@Import({KafkaConsumerConfig.class, KafkaProducerConfig.class})
public class KafkaModuleConfig {
}
