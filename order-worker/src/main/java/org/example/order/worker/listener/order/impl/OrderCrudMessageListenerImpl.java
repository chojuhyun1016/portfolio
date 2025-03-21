package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.listener.order.OrderCrudMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCrudMessageListenerImpl implements OrderCrudMessageListener {
    private final OrderCrudMessageFacade facade;

    @Override
    @KafkaListener(topics = "#{@orderCrudTopic}", groupId = "order-order-crud", containerFactory = "kafkaBatchListenerContainerFactory", concurrency = "10")
    public void executeOrderCrud(List<ConsumerRecord<String, Object>> records, Acknowledgment acknowledgment) {
        log.debug("order-crud records size : {}", records.size());

        try {
            records.stream().map(ConsumerRecord::value).forEach(value -> log.info("{}", value));
            facade.executeOrderCrud(records);
        } catch (Exception e) {
            log.error("error : order-crud", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
