package org.example.order.client.kafka.config.topic;

import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka topic 설정 프로퍼티 바인딩용 구성 클래스
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {
}
