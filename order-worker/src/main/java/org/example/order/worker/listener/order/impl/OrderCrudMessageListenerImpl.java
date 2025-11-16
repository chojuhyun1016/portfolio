package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.messaging.ConsumerEnvelope;
import org.example.order.common.support.logging.Correlate;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.worker.dto.consumer.OrderCrudConsumerDto;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.listener.order.OrderCrudMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OrderCrudMessageListenerImpl
 * - CRUD 메시지 배치 수신
 * - 각 레코드를 Envelope로 감싸 파사드에 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCrudMessageListenerImpl implements OrderCrudMessageListener {

    private final OrderCrudMessageFacade facade;

    private static final String DEFAULT_TYPE =
            "org.example.order.contract.order.messaging.event.OrderCrudMessage";

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
            key = "#records != null && #records.size() == 1 && #records[0].value() != null && #records[0].value().payload() != null && #records[0].value().payload().orderId() != null ? #records[0].value().payload().orderId() : T(java.util.UUID).randomUUID().toString()",
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void executeOrderCrud(List<ConsumerRecord<String, OrderCrudMessage>> records,
                                 Acknowledgment acknowledgment) {
        log.info("order-crud records size: {}", records.size());

        for (ConsumerRecord<String, OrderCrudMessage> r : records) {
            log.info("order-crud record: key={} value={}", r.key(), r.value());
        }

        try {
            List<ConsumerEnvelope<OrderCrudConsumerDto>> envelopes = records.stream()
                    .map(r -> {
                        OrderCrudMessage msg = r.value();

                        if (msg == null || msg.payload() == null) {
                            return null;
                        }

                        return ConsumerEnvelope.fromRecord(r, OrderCrudConsumerDto.from(msg));
                    })
                    .filter(e -> e != null)
                    .toList();

            log.info("order-crud envelopes: {}", envelopes.size());

            if (envelopes.isEmpty()) {
                log.warn("order-crud: all envelopes are invalid. size={}", records.size());

                return;
            }

            facade.executeOrderCrud(envelopes);
        } catch (Exception e) {
            log.error("error: order-crud", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
