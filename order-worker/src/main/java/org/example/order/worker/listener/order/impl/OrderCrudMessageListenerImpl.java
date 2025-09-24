package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.listener.order.OrderCrudMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

import org.example.order.common.support.logging.Correlate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCrudMessageListenerImpl implements OrderCrudMessageListener {

    private final OrderCrudMessageFacade facade;

    private static final String DEFAULT_TYPE =
            "org.example.order.core.infra.messaging.order.message.OrderCrudMessage";

    @Override
    @KafkaListener(
            topics = "#{@orderCrudTopic}",
            groupId = "group-order-crud",
            containerFactory = "kafkaBatchListenerContainerFactory",
            concurrency = "10",
            properties = {
                    "spring.json.value.default.type=" + DEFAULT_TYPE
            }
    )
    @Correlate(
            key = "#records != null && #records.size() == 1 ? #records[0].value?.dto?.order?.orderId : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void executeOrderCrud(List<ConsumerRecord<String, OrderCrudMessage>> records, Acknowledgment acknowledgment) {

        log.info("order-crud records size : {}", records.size());

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
