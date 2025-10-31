package org.example.order.batch.config;

import lombok.RequiredArgsConstructor;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KafkaListenerTopicConfig
 * - 토픽 이름 Bean 주입 (SpEL로 @KafkaListener 등에서 사용 가능)
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaListenerTopicConfig {

    @Bean
    public String orderLocalTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_LOCAL);
    }

    @Bean
    public String orderApiTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_API);
    }

    @Bean
    public String orderCrudTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_CRUD);
    }

    @Bean
    public String orderRemoteTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_REMOTE);
    }

    @Bean
    public String orderDlqTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_DLQ);
    }

    @Bean
    public String orderAlarmTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_ALARM);
    }
}
