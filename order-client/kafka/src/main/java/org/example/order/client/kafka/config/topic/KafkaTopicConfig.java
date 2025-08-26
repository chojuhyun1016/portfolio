package org.example.order.client.kafka.config.topic;

import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 큰 맥락
 * - Kafka 토픽 관련 설정을 Spring 환경에 바인딩하기 위한 구성 클래스.
 * - application.yml 등에서 정의된 `kafka.topic` 속성을
 * {@link KafkaTopicProperties} 로 주입 가능하게 한다.
 * - 별도의 빈 정의는 없고, @EnableConfigurationProperties 만 활성화한다.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {
}
