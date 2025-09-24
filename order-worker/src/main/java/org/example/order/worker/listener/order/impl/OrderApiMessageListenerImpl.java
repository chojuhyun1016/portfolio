package org.example.order.worker.listener.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.core.infra.messaging.order.message.OrderApiMessage;
import org.example.order.worker.facade.order.OrderApiMessageFacade;
import org.example.order.worker.listener.order.OrderApiMessageListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.example.order.common.support.logging.Correlate;

/**
 * OrderApiMessageListenerImpl
 * ------------------------------------------------------------------------
 * 목적
 * - API 요청 메시지 수신.
 * MDC 전략
 * - MdcRecordInterceptor: 헤더 traceId 복원 + key → orderId (+ traceId fallback).
 * - @Correlate: 메시지 바디의 id를 추출해 traceId/orderId를 “무조건” 보강.
 * <p>
 * [변경 사항]
 * - @Correlate: 단일 SpEL → 다중 경로(paths) 순차 탐색으로 유연성 강화(바디/키/헤더 모두 지원).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApiMessageListenerImpl implements OrderApiMessageListener {

    private static final String DEFAULT_TYPE =
            "org.example.order.core.infra.messaging.order.message.OrderApiMessage";

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
                    // 바디
                    "#p0?.value()?.id",
                    "#p0?.value()?.orderId",
                    "#p0?.value()?.payload?.id",
                    // 키
                    "#p0?.key()",
                    // 헤더
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
            facade.requestApi(record.value());
        } catch (Exception e) {
            log.error("error : order-api", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
