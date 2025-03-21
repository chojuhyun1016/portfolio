package org.example.order.client.kafka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({KafkaConsumerConfig.class, KafkaProducerConfig.class})
public class KafkaConfig {
}
