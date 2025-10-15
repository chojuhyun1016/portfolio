package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.worker.dto.consumer.OrderApiConsumerDto;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.listener.order.OrderApiMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.example.order.common.support.logging.Correlate;

/**
 * OrderApiMessageListenerImpl
 * - API 요청 메시지를 수신해 파사드에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageListenerImpl implements OrderApiMessageListener {

    private static final String DEFAULT_TYPE =
            "org.example.order.contract.order.messaging.event.OrderApiMessage";

    private final OrderApiMessageFacade facade;

    @Override
    @KafkaListener(
            topics = "#{@orderApiTopic}",
            groupId = "group-order-api",
            concurrency = "2",
            properties = {
                    "spring.json.value.default.type=" + DEFAULT_TYPE
            }
    )
    @Correlate(
            paths = {
                    "#p0?.value()?.id",
                    "#p0?.value()?.orderId",
                    "#p0?.value()?.payload?.id",
                    "#p0?.key()",
                    "#p0?.headers()?.get('orderId')",
                    "#p0?.headers()?.get('traceId')",
                    "#p0?.headers()?.get('X-Request-Id')",
                    "#p0?.headers()?.get('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void orderApi(ConsumerRecord<String, OrderApiMessage> record, Acknowledgment acknowledgment) {
        log.info("API - order-api record received: {}", record.value());

        try {
            OrderApiConsumerDto dto = OrderApiConsumerDto.from(record.value());

            facade.requestApi(dto);
        } catch (Exception e) {
            log.error("error : order-api", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
