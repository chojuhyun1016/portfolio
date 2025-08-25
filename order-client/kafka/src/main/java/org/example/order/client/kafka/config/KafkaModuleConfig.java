package org.example.order.client.kafka.config;

import org.example.order.client.kafka.config.consumer.KafkaConsumerConfig;
import org.example.order.client.kafka.config.producer.KafkaProducerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 모듈 묶음 구성
 * - 개별 Config 들이 @ConditionalOnProperty 로 제어되므로
 *   이 클래스는 그냥 Import 역할만 수행
 */
@Configuration
@Import({KafkaConsumerConfig.class, KafkaProducerConfig.class})
public class KafkaModuleConfig {
}
