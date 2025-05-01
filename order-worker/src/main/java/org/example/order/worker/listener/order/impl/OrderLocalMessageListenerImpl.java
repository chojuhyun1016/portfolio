package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.code.type.MessageCategory;
import org.example.order.worker.facade.order.OrderLocalMessageFacade;
import org.example.order.worker.listener.order.OrderLocalMessageListener;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocalMessageListenerImpl implements OrderLocalMessageListener {
    private final OrderLocalMessageFacade facade;

    @Override
    @KafkaListener(topics = "#{@orderLocalTopic}", groupId = "order-order-local", concurrency = "2")
    public void orderLocal(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        MDC.put("category", MessageCategory.ORDER_LOCAL.getCode());
        log.info("{}", record.value());

        try {
            facade.sendOrderApiTopic(record);
        } catch (Exception e) {
            log.error("error : order-local", e);
        } finally {
            MDC.clear();
            acknowledgment.acknowledge();
        }
    }
}
