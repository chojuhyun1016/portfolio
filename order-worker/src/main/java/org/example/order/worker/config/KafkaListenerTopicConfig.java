package org.example.order.worker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.property.KafkaTopicProperties;
import org.example.order.common.code.enums.MessageCategory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaListenerTopicConfig {

    @Bean
    public String orderLocalTopic(KafkaTopicProperties kafkaTopicProperties) {
        return kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL);
    }

    @Bean
    public String orderApiTopic(KafkaTopicProperties kafkaTopicProperties) {
        return kafkaTopicProperties.getName(MessageCategory.ORDER_API);
    }

    @Bean
    public String orderCrudTopic(KafkaTopicProperties kafkaTopicProperties) {
        return kafkaTopicProperties.getName(MessageCategory.ORDER_CRUD);
    }

    @Bean
    public String orderRemoteTopic(KafkaTopicProperties kafkaTopicProperties) {
        return kafkaTopicProperties.getName(MessageCategory.ORDER_REMOTE);
    }
}
