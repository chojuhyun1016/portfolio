package org.example.order.worker.config;

import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaListenerTopicConfig {

    @Bean
    public String orderLocalTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_LOCAL);
    }

    @Bean
    public String orderApiTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_API);
    }

    @Bean
    public String orderCrudTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_CRUD);
    }

    @Bean
    public String orderRemoteTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_REMOTE);
    }

    @Bean
    public String orderDlqTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_DLQ);
    }

    @Bean
    public String orderAlarmTopic(KafkaTopicProperties props) {
        return props.getName(MessageOrderType.ORDER_ALARM);
    }
}
