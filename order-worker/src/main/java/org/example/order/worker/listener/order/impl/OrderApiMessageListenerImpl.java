package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.listener.order.OrderApiMessageListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class OrderApiMessageListenerImpl implements OrderApiMessageListener {
    private final OrderApiMessageFacade facade;

    @Override
    @KafkaListener(topics = "#{@orderApiTopic}", groupId = "order-order-api", concurrency = "2")
    public void orderApi(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        log.debug("API - order-api record received: {}", record);

        try {
            facade.requestApi(record.value());
        } catch (Exception e) {
            log.error("error : order-api", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
