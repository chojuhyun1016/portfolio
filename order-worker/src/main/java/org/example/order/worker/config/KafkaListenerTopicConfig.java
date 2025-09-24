package org.example.order.worker.config;

import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.core.infra.messaging.order.code.MessageCategory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaListenerTopicConfig {

    @Bean
    public String orderLocalTopic(KafkaTopicProperties props) {
        return props.getName(MessageCategory.ORDER_LOCAL);
    }

    @Bean
    public String orderApiTopic(KafkaTopicProperties props) {
        return props.getName(MessageCategory.ORDER_API);
    }

    @Bean
    public String orderCrudTopic(KafkaTopicProperties props) {
        return props.getName(MessageCategory.ORDER_CRUD);
    }

    @Bean
    public String orderRemoteTopic(KafkaTopicProperties props) {
        return props.getName(MessageCategory.ORDER_REMOTE);
    }
}
